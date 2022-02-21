package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.adapt.IDataAccessor;
import com.github.mrmks.mc.status.adapt.IEntityConvert;
import com.github.mrmks.mc.status.api.IResource;
import com.github.mrmks.mc.status.api.WritingStatus;
import com.github.mrmks.mc.status.utils.ByteArrayToIntMap;
import com.github.mrmks.mc.status.utils.CommonUtils;
import com.github.mrmks.mc.status.utils.IntArray;
import com.github.mrmks.mc.status.utils.IntQueue;

import java.util.Arrays;

/**
 * The T is the entity instance of your game, but we don't obtain your instance here directly,
 * we use the byte[] for instead, which is also the key when we store the data;
 */
class EntityManager<T> {

    private final TaskManager taskManager;
    private final IDataAccessor dataAccessor;
    private final IEntityConvert<T> convert;
    private final StateControl sessionControl;

    private final WrappedAttribute<T>[] attributes;
    private final WrappedAttributeProvider<T>[] providers;
    private final int resourceSize;
    private final int resourceSize2;

    private final IntQueue refreshedAry;
    private int nextIndex;
    private final ByteArrayToIntMap indexConvert;
    private StatusEntity[] entityMap;

    private final IntQueue removingQueue;

    private final int[][] taskResourceAry;
    private final int[] taskIntervals;

    private final IntArray cacheIds = new IntArray(64), cacheVs = new IntArray(64);

    EntityManager(
            TaskManager taskManager,
            IEntityConvert<T> iec,
            IDataAccessor ida,
            StateControl sessionControl,
            WrappedAttribute<T>[] attributeAry,
            int resourceSize,
            int[] taskIntervals,
            int[][] taskResourceAry,
            WrappedAttributeProvider<T>[] attributeProviderAry
    ) {
        this.dataAccessor = ida;
        this.convert = iec;
        this.sessionControl = sessionControl;
        this.taskManager = taskManager;
        this.attributes = attributeAry;
        this.resourceSize = resourceSize;
        this.resourceSize2 = resourceSize << 1;
        this.taskIntervals = taskIntervals;
        this.taskResourceAry = taskResourceAry;
        this.providers = attributeProviderAry;

        this.entityMap = new StatusEntity[1024];
        this.indexConvert = new ByteArrayToIntMap(1024);
        this.refreshedAry = new IntQueue(64);
        this.nextIndex = 1;

        this.removingQueue = new IntQueue();

        byte[] sysBytes = new byte[0];
        entityMap[0] = new StatusEntity(sysBytes, null, null, false, 0, null);
    }

    int queryAttribute(String key) {
        return CommonUtils.binarySearch(attributes, o -> o.name, key, String::compareTo);
    }

    void createEntity(T entity, boolean shouldSave) {

        // you can't create entity in session.
        if (sessionControl.shouldDenyEntityModify()) throw new IllegalStateException();

        byte[] bytes = convert.toBytes(entity);
        int index = findEntityIndex(bytes);
        // entity existed
        if (index >= 0) {
            StatusEntity se = entityMap[index];
            resumeEntity(entity, se);
            return;
        }

        index = refreshedAry.isEmpty() ? nextIndex++ : refreshedAry.remove();
        indexConvert.put(bytes, index);

        StatusTable table = new StatusTable(attributes.length);
        WritingImpl ws = new WritingImpl(attributes.length, cacheIds, cacheVs);
        for (WrappedAttributeProvider<T> wap : providers) {
            wap.update(entity, ws);
            for (int i = 0; i < ws.length; i++) table.accept(ws.ids.at(i), ws.vs.at(i));
            ws.reset();
        }

        for (int i = 0; i < resourceSize; i++) {
            IResource<T> r = (IResource<T>) attributes[i].attribute;
            int val = shouldSave ? dataAccessor.getValue(bytes, i).orElse(0) : 0;
            table.accept(i, val + r.baseValue());
            table.accept(i + resourceSize, r.baseUBound());
            table.accept((resourceSize << 1) + i, r.baseStep());
        }

        // create resource task;
        ResourceTask task = new ResourceTask(index);
        taskManager.addTask(task);

        int[][] storedData = shouldSave ? dataAccessor.getStore(bytes) : null;

        StatusEntity se = new StatusEntity(bytes, table, storedData, shouldSave, index, task);
        if (index >= entityMap.length) {
            entityMap = Arrays.copyOf(entityMap, index << 1);
        }
        entityMap[index] = se;
    }

    void resumeEntity(T entity, StatusEntity se) {
        if (entity != null && se != null && se.offline) {
            se.offline = false;
            for (int i = 0; i < resourceSize; i++) attributes[i].update(entity, se.table.get(i), 0);
        }
    }

    void removeEntity(T entity) {

        // also, you can't remove entity in session.
        if (sessionControl.shouldDenyEntityModify()) throw new IllegalStateException();

        int index = findEntityIndex(entity);
        removeEntity(index);
    }

    private void removeEntity(int index) {
        StatusEntity se = getEntity(index);
        if (se != null) {
            if (se.buffRefCount > 0) {
                se.offline = true;
            } else{
                entityMap[index] = null;
                if (se.shouldSave) {
                    StatusTable st = se.table;
                    for (int i = 0; i < resourceSize; i++) {
                        int base = ((IResource<T>) attributes[i].attribute).baseValue();
                        dataAccessor.writeValue(se.storeKey, i, st.get(i) - base);
                    }
                    dataAccessor.writeStore(se.storeKey, se.data);
                    dataAccessor.flushEntity(se.storeKey);
                }
                if (se.resourceTask != null) {
                    se.resourceTask.cancel(true);
                    se.resourceTask.onRemove();
                }
                if (index + 1 == nextIndex) {
                    --nextIndex;
                } else {
                    refreshedAry.offer(index);
                }
            }
        }
    }

    void reduceEntityRef(int id) {
        StatusEntity se = getEntity(id);
        if (se != null && se.buffRefCount > 0) {
            --se.buffRefCount;
            if (se.buffRefCount == 0 && se.offline) {
                removingQueue.offer(id);
            }
        }
    }

    boolean testEntity(T entity, int id) {
        StatusEntity se = getEntity(id);
        return se != null && Arrays.equals(se.storeKey, convert.toBytes(entity));
    }

    StatusEntity getEntity(int id) {
        return id >= 0  && id < entityMap.length ? entityMap[id] : null;
    }

    void applyResource(T entity, int index, int[] id, int[] val) {
        if (id == null || val == null || id.length != val.length || id.length == 0) return;
        StatusEntity se = getEntity(index);
        if (se == null) return;
        entity = entity == null ? convert.fromBytes(se.storeKey) : entity;
        boolean directUpdate = !se.offline && entity != null;
        int _id, _val, _prev;
        for (int i = 0; i < id.length; ++i) {
            _id = id[i];
            _val = val[i];
            if (_id < 0 || _id >= resourceSize || _val == 0) continue;

            if (_val > 0) {
                _val = Math.min(se.table.get(_id + resourceSize) - se.table.get(_id), _val);
                if (_val == 0) continue;
            }

            _prev = se.table.accept(_id, _val);
            if (directUpdate) attributes[_id].update(entity, _prev, _val);
        }
    }

    void applyAttribute(T entity, int index, int[] id, int[] val) {
        if (id == null || val == null || id.length != val.length || id.length == 0) return;
        StatusEntity se = getEntity(index);
        if (se == null) return;
        entity = entity == null ? convert.fromBytes(se.storeKey) : entity;
        boolean directUpdate = !se.offline && entity != null;
        int _id, _val, _prev, _r_id, _r_val, _r_prev;
        for (int i = 0; i < id.length; ++i) {
            _id = id[i];
            _val = val[i];
            if (_id < resourceSize || _id >= attributes.length || _val == 0) continue;

            _prev = se.table.accept(_id, _val);
            if (_id < resourceSize2 && _val < 0 && (_r_val = _prev + _val - se.table.get(_r_id = _id - resourceSize)) < 0) {
                _r_prev = se.table.accept(_r_id, _r_val);
                if (directUpdate) attributes[_r_id].update(entity, _r_prev, _r_val);
            }
            if (directUpdate) attributes[_id].update(entity, _prev, _val);
        }
    }

    int findEntityIndex(T entity) {
        return entity == null ? -1 : findEntityIndex(convert.toBytes(entity));
    }

    private int findEntityIndex(byte[] bytes) {
        return bytes == null ? -1 : indexConvert.getOrDefault(bytes, -1);
    }

    int updateProvider(T entity, String key) {
        int id = CommonUtils.binarySearch(providers, obj -> obj.name, key, String::compareTo);
        if (id >= 0) {
            updateProvider(entity, id);
        }
        return id;
    }

    void updateProvider(T e, int id) {
        if (!sessionControl.shouldDenyEntityModify() && id >= 0 && id < providers.length) {
            int index = findEntityIndex(e);
            StatusEntity se = getEntity(index);
            if (se == null) return;

            if (se.offline) resumeEntity(e, se);

            WrappedAttributeProvider<T> wap = providers[id];
            WritingImpl ws = new WritingImpl(attributes.length, cacheIds, cacheVs);
            wap.update(e, ws);
            for (int i = 0; i < ws.length; i++) se.table.accept(ws.ids.at(i), ws.vs.at(i));
        }
    }

    StatusTable.Readonly createReadonly(int index) {
        StatusEntity se = getEntity(index);
        if (se != null) {
            return se.table.readonly();
        } else {
            return null;
        }
    }

    int[] getModifierStore(int index, int storeId) {
        StatusEntity se = entityMap[index];
        if (se != null) {
            return se.getData(storeId);
        }
        return null;
    }

    byte[] getEntityKey(int index) {
        StatusEntity se = entityMap[index];
        return se == null ? null : se.storeKey;
    }

    void tick() {
        while (!removingQueue.isEmpty()) {
            removeEntity(removingQueue.remove());
        }
    }

    private static class WritingImpl implements WritingStatus {

        private final int size;
        private final IntArray ids, vs;
        private int length = 0;
        private WritingImpl(int dataSize, IntArray ids, IntArray vs) {
            this.size = dataSize;
            this.ids = ids;
            this.vs = vs;
        }

        @Override
        public void write(int id, int val) {
            if (id < 0 || id >= size || id > Short.MAX_VALUE) return;
            int index = ids.indexOf(id, length);
            if (index < 0) {
                (ids.size() == length ? ids.enlarge(length) : ids).set(length, id);
                (vs.size() == length ? vs.enlarge(length) : vs).set(length, val);
                length ++;
            } else {
                vs.set(index, vs.at(index) + val);
            }
        }

        private void reset() {
            length = 0;
        }
    }

    private class ResourceTask extends TaskManager.Task {
        private final int eid;
        private final int[] remainingTicks = Arrays.copyOf(taskIntervals, taskIntervals.length);

        private ResourceTask(int id) {
            this.eid = id;
        }

        @Override
        public int run(int it, boolean reset) {
            StatusEntity se = entityMap[eid];
            if (se == null) {
                cancel(true);
                return -1;
            }

            StatusTable table = se.table;
            int re = Integer.MAX_VALUE;
            for (int i = 0; i < remainingTicks.length; i++) {
                remainingTicks[i] -= it + 1;
                if (remainingTicks[i] < 0) {
                    remainingTicks[i] = taskIntervals[i];
                    int[] modifies = new int[taskResourceAry.length];
                    for (int id : taskResourceAry[i]) {
                        modifies[i] = table.get(id + resourceSize2);
                    }
                    applyResource(null, eid, taskResourceAry[i], modifies);
                }
                re = Math.min(re, remainingTicks[i]);
            }
            return re;
        }
    }

}
