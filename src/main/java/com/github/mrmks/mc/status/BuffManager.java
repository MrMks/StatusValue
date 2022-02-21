package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.adapt.IDataAccessor;
import com.github.mrmks.mc.status.adapt.IGuiCallback;
import com.github.mrmks.mc.status.api.BuffType;
import com.github.mrmks.mc.status.utils.IntQueue;

import java.lang.ref.WeakReference;
import java.util.Arrays;

/*
 * Buff Design.
 * There are three kinds of buffs you can use in this program.
 * One to resource, one to attribute and one to both, via modifier.
 *
 * When count or duration is a minus, or bigger than 60 * 60 * Constants.TICKS_PER_SEC,
 * we will take it as an un-auto-stoppable buff. In this case, we would not refer this
 * buff to the src entity. If the buff is to modifier, we will remove the buff immediately
 * after we remove the StatusEntity.
 *
 * So only use un-auto-stoppable buff for resource or attribute is recommended.
 *
 * Besides, we will not save any buff info when we're shutting down, means all buffs
 * will be removed immediately(onRemove methods will be invoked).
 */

class BuffManager<T> {

    private HandlerList<T> handlerList;
    private EntityManager<T> entityManager;
    private final TaskManager taskManager;
    private final IGuiCallback guiCallback;
    private final int GUI_UPDATE_INTERVAL;

    private BuffTask[][] buffAry;
    private int size = 0;
    private final IntQueue released = new IntQueue();
    private final int cap = Constants.DEFAULT_ARRAY_INIT_CAPACITY_FACTOR;
    private final int maximumTickLimit = Constants.TICKS_PER_SEC * 3600;

    BuffManager(
            TaskManager taskManager,
            IGuiCallback guiCallback,
            ConfigMap configMap
    ) {
        this.taskManager = taskManager;
        this.guiCallback = guiCallback;
        this.GUI_UPDATE_INTERVAL = configMap.getGuiInterval() * Constants.TICKS_PER_SEC;
    }

    void setEntityManager(EntityManager<T> em) {
        if (this.entityManager == null) this.entityManager = em;
    }

    void setHandlerList(HandlerList<T> hl) {
        if (this.handlerList == null) this.handlerList = hl;
    }

    // handled resource
    void addBuff(StatusEntity tar, StatusEntity src, BuffData data,
                 T tarE, T srcE, int interval, int count, int[] tarId, int[] tarVal, int[] srcId, int[] srcVal
    ) {

        if (count == 0 || interval < 0) return;
        if (tarId == null || tarVal == null || tarId.length != tarVal.length) tarId = tarVal = null;
        if (srcId == null || srcVal == null || srcId.length != srcVal.length) srcId = srcVal = null;
        if (tarId == null && srcId == null) return;

        if (count < 0 || count >= maximumTickLimit || count + interval * count >= maximumTickLimit) count = -1;
        if (count > 0) src.buffRefCount++;

        BuffTask task = matchBuff(tar, data, src.storeKey);

        if (task != null) {
            int sA = srcId == null ? 0 : srcId.length;
            int sB = tarId == null ? 0 : tarId.length;

            int[] args = new int[sA + sB + 2];

            args[0] = sA;
            args[1] = sB;
            if (sA > 0) {
                System.arraycopy(srcId, 0, args, 2, sA);
                System.arraycopy(srcVal, 0, args, 2 + sA, sA);
            }
            if (sB > 0) {
                System.arraycopy(tarId, 0, args, 2 + (sA << 1), sB);
                System.arraycopy(tarVal, 0, args, 2 + (sA << 1) + sB, sB);
            }

            expandTask(task, data, srcE, src.storeKey, src.id, interval, count, args);
        } else {
            int bid = nextId();
            int ebid = tar.assignBuffId(bid);

            task = new HandledResourceBuff(tar.id, ebid, src.id, tar.storeKey, src.storeKey, data,
                    interval, count, tarE, srcE, srcId, tarId, srcVal, tarVal);

            addBuff0(bid, task);
        }

    }

    void addBuff(StatusEntity tar, StatusEntity src, BuffData data, T tarE, T srcE, int interval, int count, int id, int[] val) {

        if (interval < 0 || count == 0 || !handlerList.isModifierValid(id)) return;
        if (count < 0 || count >= maximumTickLimit || count + interval * count >= maximumTickLimit)
            count = -1;
        if (count > 0) src.buffRefCount++;

        BuffTask task = matchBuff(tar, data, src.storeKey);

        if (task != null) {
            int l = val == null ? 0 : val.length;

            int[] args = new int[2 + l];
            args[0] = id;
            args[1] = l;
            if (l > 0) System.arraycopy(val, 0, args, 2, l);

            expandTask(task, data, srcE, src.storeKey, src.id, interval, count, args);
        } else {
            int bid = nextId();
            int ebid = tar.assignBuffId(bid);

            task = new SourceResourceBuff(tar.id, ebid, src.id, tar.storeKey, src.storeKey, data, interval, count, tarE, srcE, id, val == null ? Constants.EMPTY_ARY_INT : val);

            addBuff0(bid, task);
        }
    }

    void addBuff(StatusEntity tar, StatusEntity src, BuffData data,
                 T tarE, T srcE, int duration, int[] tarId, int[] tarVal, int[] srcId, int[] srcVal
    ) {

        if (duration == 0) return;
        if (tarId == null || tarVal == null || tarId.length != tarVal.length || tarId.length == 0) tarId = tarVal = null;
        if (srcId == null || srcVal == null || srcId.length != srcVal.length || srcId.length == 0) srcId = srcVal = null;
        if (tarId == null && srcId == null) return;

        if (duration < 0 || duration >= maximumTickLimit) duration = -1;
        if (duration > 0) src.buffRefCount++;

        BuffTask task = matchBuff(tar, data, src.storeKey);

        int count = duration < 0 ? -1 : 2;
        int interval = duration < 0 ? Integer.MAX_VALUE : duration;

        if (task != null) {
            int sA = srcId == null ? 0 : srcId.length;
            int sB = tarId == null ? 0 : tarId.length;

            int[] args = new int[sA + sB + 2];

            args[0] = sA;
            args[1] = sB;
            if (sA > 0) {
                System.arraycopy(srcId, 0, args, 2, sA);
                System.arraycopy(srcVal, 0, args, 2 + sA, sA);
            }
            if (sB > 0) {
                System.arraycopy(tarId, 0, args, 2 + (sA << 1), sB);
                System.arraycopy(tarVal, 0, args, 2 + (sA << 1) + sB, sB);
            }

            expandTask(task, data, srcE, src.storeKey, src.id, interval, count, args);
        } else {
            int bid = nextId();
            int ebid = tar.assignBuffId(bid);

            task = new AttributeBuff(tar.id, ebid, src.id, tar.storeKey, src.storeKey, data,
                    interval, count, tarE, srcE, srcId, tarId, srcVal, tarVal);

            addBuff0(bid, task);
        }

    }

    @SuppressWarnings("unchecked")
    private void addBuff0(int bid, BuffTask task) {
        if (buffAry == null) buffAry = new BuffManager.BuffTask[1][];
        int i = bid >>> cap, j = bid & (1 << cap) - 1;
        if (i == buffAry.length) {
            buffAry = Arrays.copyOf(buffAry, i << 1);
            buffAry[i] = new BuffManager.BuffTask[1 << cap];
        }
        buffAry[i][j] = task;
        taskManager.addTask(task);
    }

    void removeBuff(StatusEntity se, BuffType type, String key, String tag, byte[] src, boolean anySource, boolean once, boolean force) {
        for (int count = 0; count < se.buffs.length ; ++count) {
            int id = se.buffs[count];
            BuffTask bt = getBuff(id, se.id, count);
            if (bt != null) {
                boolean isMatch = (type == null || bt.type == null || type == bt.type)
                        && (key == null || key.equals(bt.key))
                        && (tag == null || tag.equals(bt.tag))
                        && (anySource || src == null || bt.src == null || Arrays.equals(src, bt.src))
                        && (force || bt.canRemove);
                if (isMatch) {
                    bt.cancel(true);
                    bt.onRemove();
                    emptyBuff(id);
                    if (once) break;
                }
            }
        }
    }

    void removeBuffAll(StatusEntity se) {
        int count = 0;
        for (int id : se.buffs) {
            BuffTask bt = getBuff(id, se.id, count);
            if (bt != null) {
                bt.cancel(true);
                bt.onRemove();
                emptyBuff(id);
            }
            ++count;
        }
        Arrays.fill(se.buffs, -1);
        se.refreshedBuff.clear();
        se.buffSize = 0;
    }

    private BuffTask matchBuff(StatusEntity se, BuffData data, byte[] src) {
        int count = 0;
        for (int id : se.buffs) {
            BuffTask bt = getBuff(id, se.id, count);
            if (bt != null && bt.key.equals(data.key) && bt.anySource == data.anySrc && (bt.anySource || Arrays.equals(bt.src, src))) {
                return bt;
            }
        }
        return null;
    }

    private void expandTask(BuffTask task, BuffData data, T srcE, byte[] src, int srcI, int interval, int count, int[] args) {
        if (task.logicCount >= 0) entityManager.reduceEntityRef(task.eidSrc);

        task.expandBy(data, srcE, src, srcI, interval, count, args);
    }

    private BuffTask getBuff(int id, int eId, int eBuffId) {
        BuffTask task = getBuff0(id);
        return task != null && task.eidTar == eId && task.ebidTar == eBuffId ? task : null;
    }

    private BuffTask getBuff0(int id) {
        if (id < 0 || id >= size) return null;
        return buffAry[id >>> cap][id & (1 << cap) - 1];
    }

    private void emptyBuff(int id) {
        if (id < 0 || id >= size) return;
        buffAry[id >>> cap][id & (1 << cap) - 1] = null;
        if (id + 1 == size) size--;
        else released.offer(id);
    }

    private int nextId() {
        return released.isEmpty() ? size++ : released.remove();
    }

    private abstract class BuffTask extends TaskManager.Task {
        protected int eidTar, ebidTar, eidSrc;
        protected String key, tag, icon;
        protected byte[] tar, src;
        protected boolean canRemove, anySource;
        protected BuffType type;


        private int guiRemaining;

        private int logicInterval;
        private int logicRemaining;
        private int logicCount;

        protected BuffTask(int eid, int ebid, int eidSrc, byte[] tar, byte[] src, BuffData data, int interval, int count) {
            this.eidTar= eid;
            this.ebidTar = ebid;
            this.eidSrc = eidSrc;
            this.key = data.key;
            this.tag = data.tag;
            this.icon = data.icon;
            this.canRemove = data.canRemove;
            this.anySource = data.anySrc;
            this.tar = tar;
            this.src = src;
            this.type = data.type;

            this.logicInterval = interval;
            this.logicCount = count;
        }

        @Override
        final int run(int interval, boolean reset) {
            if (!reset) {
                logicRemaining -= interval + 1;
                guiRemaining -= interval + 1;
            } else {
                logicRemaining -= 1;
                guiRemaining -= 1;
            }

            if (logicRemaining < 0) {
                runLogic();
                logicRemaining = logicInterval;
                if (logicCount > 0) --logicCount;
                if (logicCount == 0) return -1;
            }
            if (guiRemaining < 0) {
                guiRemaining = GUI_UPDATE_INTERVAL;
                guiCallback.updateBuff(tar, key, src, anySource, icon, logicInterval * (logicCount - 1) + logicRemaining);
            }
            return Math.min(guiRemaining, logicRemaining);
        }

        abstract void runLogic();

        @Override
        final void onRemove() {
            if (logicCount >= 0) entityManager.reduceEntityRef(eidSrc);
            entityManager.getEntity(eidTar).freeBuffId(ebidTar);
            guiCallback.removeBuff(tar, key, src, anySource);
            onRemoveLogic();
        }

        protected void onRemoveLogic() {}

        final void expandBy(BuffData data, T srcE, byte[] src, int eidSrc, int interval, int count, int... args) {
            resetRemaining();
            this.src = src;
            this.eidSrc = eidSrc;
            this.tag = data.tag;
            this.icon = data.icon;
            this.canRemove = data.canRemove;
            this.logicRemaining = 0;
            this.guiRemaining = 0;
            this.logicCount = count;
            this.logicInterval = interval;
            expandBy(srcE, args);
        }

        abstract void expandBy(T srcE, int... args);

    }

    private class HandledResourceBuff extends BuffTask {

        private final WeakReference<T> tarE;
        private WeakReference<T> srcE;
        private int[] srcId, tarId, srcVal, tarVal;

        protected HandledResourceBuff(int eid, int ebid, int eidSrc, byte[] tar, byte[] src, BuffData data, int interval, int count,
                                      T tarE, T srcE, int[] srcId, int[] tarId, int[] srcVal, int[] tarVal) {
            super(eid, ebid, eidSrc, tar, src, data, interval, count);
            this.tarE = new WeakReference<>(tarE);
            this.srcE = new WeakReference<>(srcE);
            this.srcId = srcId;
            this.tarId = tarId;
            this.srcVal = srcVal;
            this.tarVal = tarVal;
        }

        @Override
        protected void runLogic() {
            T src = srcE.get(), tar = tarE.get();
            if (src != null && srcId != null && srcVal != null) {
                entityManager.applyResource(src, eidSrc, srcId, srcVal);
            }
            if (tar != null && tarId != null && tarVal != null) {
                entityManager.applyResource(tar, eidTar, tarId, tarVal);
            } else {
                cancel(true);
                onRemove();
            }
        }

        @Override
        void expandBy(T srcE, int... args) {
            this.srcE = new WeakReference<>(srcE);
            int sizeA = args[0], sizeB = args[1];
            if (sizeA == 0) {
                srcId = srcVal = null;
            } else {
                srcId = Arrays.copyOfRange(args, 2, 2 + sizeA);
                srcVal = Arrays.copyOfRange(args, 2 + sizeA, 2 + (sizeA << 1));
            }
            if (sizeB == 0) {
                tarId = tarVal = null;
            } else {
                tarId = Arrays.copyOfRange(args, 2 + (sizeA << 1), 2 + sizeB + (sizeA << 1));
                tarVal = Arrays.copyOfRange(args, 2 + sizeB + (sizeA << 1), 2 + (sizeB << 1) + (sizeA << 1));
            }
        }
    }

    private class SourceResourceBuff extends BuffTask {
        private final WeakReference<T> tar;
        private WeakReference<T> src;
        private int id;
        private int[] val;
        protected SourceResourceBuff(int eid, int ebid, int eidSrc, byte[] tar, byte[] src, BuffData data, int interval, int count,
                                     T tarE, T srcE, int id, int[] val) {
            super(eid, ebid, eidSrc, tar, src, data, interval, count);
            this.tar = new WeakReference<>(tarE);
            this.src = new WeakReference<>(srcE);
            this.id = id;
            this.val = val;
        }

        @Override
        void runLogic() {
            T tar = this.tar.get(), src = this.src.get();
            handlerList.beginTransaction(eidSrc, eidTar, src, tar).modify(id, val);
        }

        @Override
        void expandBy(T srcE, int... args) {
            this.src = new WeakReference<>(srcE);
            this.id = args[0];
            int l = args[1];
            val = l == 0 ? Constants.EMPTY_ARY_INT : Arrays.copyOfRange(args, 2, l);
        }
    }

    /*
     * This buff do not perform applications, but perform restores.
     */
    private class AttributeBuff extends BuffTask {
        private final WeakReference<T> tar;
        private WeakReference<T> src;
        private int[] tarId, tarVal, srcId, srcVal;
        private boolean second = false;
        protected AttributeBuff(int eid, int ebid, int eidSrc, byte[] tar, byte[] src, BuffData data, int interval, int count,
                                T tarE, T srcE, int[] tarId, int[] tarVal, int[] srcId, int[] srcVal) {
            super(eid, ebid, eidSrc, tar, src, data, interval, count);
            this.tar = new WeakReference<>(tarE);
            this.src = new WeakReference<>(srcE);
            this.tarId = tarId;
            this.tarVal = tarVal;
            this.srcId = srcId;
            this.srcVal = srcVal;
        }

        @Override
        void runLogic() {
            if (!second) second = true;
            else {
                if (tarId != null) {
                    entityManager.applyAttribute(tar.get(), eidTar, tarId, tarVal);
                }
                if (srcId != null) {
                    entityManager.applyAttribute(src.get(), eidSrc, srcId, srcVal);
                }
            }
        }

        @Override
        void expandBy(T srcE, int... args) {
            second = false;
            this.src = new WeakReference<>(srcE);
            int sizeA = args[0], sizeB = args[1];
            if (sizeA == 0) {
                srcId = srcVal = null;
            } else {
                srcId = Arrays.copyOfRange(args, 2, 2 + sizeA);
                srcVal = Arrays.copyOfRange(args, 2 + sizeA, 2 + (sizeA << 1));
            }
            if (sizeB == 0) {
                tarId = tarVal = null;
            } else {
                tarId = Arrays.copyOfRange(args, 2 + (sizeA << 1), 2 + sizeB + (sizeA << 1));
                tarVal = Arrays.copyOfRange(args, 2 + sizeB + (sizeA << 1), 2 + (sizeB << 1) + (sizeA << 1));
            }
        }
    }
}
