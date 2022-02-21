package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.api.ModificationEvent;
import com.github.mrmks.mc.status.utils.CommonUtils;
import com.github.mrmks.mc.status.utils.IntMap;

import java.util.*;
import java.util.function.ToIntFunction;

public class HandlerList<T> {

    private final WrappedModifier[] modifierAry;
    private final IntMap<WrappedHandler[]> handlerAry;
    private final HashMap<String, WrappedHandler> handlerMap;
    private final int resourceSize;

    private final int[] paramSizeMap;

    //private final int[][] dataCopySrc, dataCopyTar;

    private final EntityManager<T> entityManager;
    private final BuffManager<T> buffManager;

    private int sessionId = 0;
    private final StateControl sessionControl;
    private final Queue<EventDelay<T>> queuedEvents = new LinkedList<>();
    private final LinkedList<TransactionImpl> queuedTransactions = new LinkedList<>();
    private final LinkedList<ModCache<T>> queuedModification = new LinkedList<>();

    protected HandlerList(
            StateControl sessionControl,
            EntityManager<T> entityManager,
            BuffManager<T> buffManager,
            IntMap<WrappedHandler[]> handlerAry,
            WrappedModifier[] modifierAry,
            HashMap<String, WrappedHandler> handlerMap,
            int[] paramSizeMap,
            int resourceSize
    ) {
        this.sessionControl = sessionControl;
        this.handlerAry = handlerAry;
        this.modifierAry = modifierAry;
        this.entityManager = entityManager;
        this.buffManager = buffManager;
        this.handlerMap = handlerMap;
        this.paramSizeMap = paramSizeMap;
        this.resourceSize = resourceSize;
    }

    int queryHandler(String key) {
        WrappedHandler wh = handlerMap.get(key);
        return wh == null ? -1 : wh.paramIndex;
    }

    short queryModifier(String key) {
        return (short) CommonUtils.binarySearch(modifierAry, o -> o.name, key, String::compareTo);
    }

    void beginSession() {
        if (sessionControl.beginSession()) clearQueue();
    }

    void finishSession() {
        if (sessionControl.canFinishSession()) {
            for (Iterator<ModCache<T>> iterator = queuedModification.iterator(); iterator.hasNext(); iterator.remove()) {
                ModCache<T> entry = iterator.next();

                T src = entry.src, tar = entry.tar;
                int srcI = entry.srcI, tarI = entry.tarI;
                ModificationTable mt = entry.mTable;

                if (!entityManager.testEntity(src, srcI) || !entityManager.testEntity(tar, tarI)) continue;

                for (ModificationTable.BuffCache bc : entry.mTable.buffCaches) {
                    if (bc.adding) {
                        // add buff
                        if (bc.reverse) {
                            src = tar;
                            tar = entry.src;

                            srcI = tarI;
                            tarI = entry.srcI;
                        }
                        if (bc.attributeOrOnce) {
                            // attribute buff
                            entityManager.applyAttribute(tar, tarI, bc.idTar, bc.valTar);
                            entityManager.applyAttribute(src, srcI, bc.idSrc, bc.valSrc);
                            buffManager.addBuff(
                                    entityManager.getEntity(tarI),
                                    entityManager.getEntity(srcI),
                                    new BuffData(bc.type, bc.key, bc.tag, bc.icon, bc.canRemoveOrForce, bc.anySource),
                                    tar, src, bc.args[0], bc.idTar, bc.valTar, bc.idSrc, bc.valSrc
                            );
                        } else {
                            // resource buff
                            if (bc.asSource) {
                                // as source
                                buffManager.addBuff(
                                        entityManager.getEntity(tarI),
                                        entityManager.getEntity(srcI),
                                        new BuffData(bc.type, bc.key, bc.tag, bc.icon, bc.canRemoveOrForce, bc.anySource),
                                        tar, src, bc.args[0], bc.args[1], bc.idTar[0], bc.valTar
                                );
                            } else {
                                // as handled
                                buffManager.addBuff(
                                        entityManager.getEntity(tarI),
                                        entityManager.getEntity(srcI),
                                        new BuffData(bc.type, bc.key, bc.tag, bc.icon, bc.canRemoveOrForce, bc.anySource),
                                        tar, src, bc.args[0], bc.args[1], bc.idTar, bc.valTar, bc.idSrc, bc.valSrc
                                );
                            }
                        }
                    } else {
                        // remove buff
                        if (bc.reverse) {
                            srcI = tarI;
                            tarI = entry.srcI;
                        }
                        buffManager.removeBuff(
                                entityManager.getEntity(tarI),
                                bc.type, bc.key, bc.tag, entityManager.getEntityKey(srcI), bc.anySource, bc.attributeOrOnce, bc.canRemoveOrForce);
                    }
                }
                entityManager.applyResource(src, srcI, mt.srcId.toArray(), mt.srcVal.toArray());
                entityManager.applyResource(tar, tarI, mt.tarId.toArray(), mt.tarVal.toArray());
            }

            clearQueue();
            sessionId ++;
            sessionControl.finishSession();
        }
    }

    void finishAutoSession() {
        if (sessionControl.canFinishAutoSession()) finishSession();
    }

    Transaction beginTransaction(T src, T tar) {
        if (!sessionControl.isInSession()) clearQueue();
        if (!sessionControl.beginAutoSession()) throw new IllegalStateException();

        int srcI = entityManager.findEntityIndex(src);
        int tatI = entityManager.findEntityIndex(tar);
        if ((srcI | tatI) < 0) throw new IllegalStateException();

        TransactionImpl trImpl = new TransactionImpl(src, tar, srcI, tatI, sessionId, false);
        queuedTransactions.add(trImpl);
        return trImpl;
    }

    /*
     * This method should only be invoked by modifier buff.
     */
    Transaction beginTransaction(int src, int tar, T srcE, T tarE) {
        if (!sessionControl.isInSession()) clearQueue();
        if (!sessionControl.beginAutoSession()) throw new IllegalStateException();

        TransactionImpl trImpl = new TransactionImpl(srcE, tarE, src, tar, sessionId, true);
        queuedTransactions.add(trImpl);
        return trImpl;
    }

    void tick() {
        clearQueue();
        sessionId ++;
        sessionControl.finishSession();
    }

    private void clearQueue() {
        queuedEvents.clear();
        queuedModification.clear();
        for (TransactionImpl tr : queuedTransactions) {
            tr.outdated = true;
            tr.srcE = tr.tarE = null;
        }
        queuedTransactions.clear();
    }

    void callEvent(T src, T tar, int srcI, int tarI, StatusTable.Readonly srcR, StatusTable.Readonly tarR,
                           int mId, int[] mVal, IntMap<int[]> hParams) {
        if (sessionControl.canCallEvent()) {
            if (sessionControl.canCallEventDirect()) {
                callEvent0(src, tar, srcI, tarI, srcR, tarR, mId, mVal, hParams);
                while (!queuedEvents.isEmpty()) {
                    EventDelay<T> e = queuedEvents.remove();
                    callEvent0(e.srcRf, e.tarRf, e.srcI, e.tarI, e.srcR, e.tarR, e.modifierId, e.args, e.paramMap);
                }
            } else {
                queuedEvents.add(new EventDelay<>(src, tar, srcI, tarI, srcR, tarR, mId, mVal, hParams));
            }
        }
    }

    boolean isModifierValid(int id) {
        return id >= 0 && id < modifierAry.length;
    }

    private void callEvent0(T src, T tar, int srcI, int tarI, StatusTable.Readonly srcR, StatusTable.Readonly tarR,
                            int mId, int[] mVal, IntMap<int[]> hParams) {
        sessionControl.beginHandler();
        WrappedHandler[] whs = handlerAry.get(mId);
        if (whs != null) {
            EventImpl mEvent = new EventImpl(src, tar, srcR, tarR, mVal);
            for (WrappedHandler wh : whs) {
                wh.handle((short) mId, mEvent, hParams.getOrDefault(wh.paramIndex, Constants.EMPTY_ARY_INT));
            }
        }
        sessionControl.beginModifier();
        ModificationTable mTable = new ModificationTable(srcR, tarR, resourceSize);
        WrappedModifier wm = modifierAry[mId];
        wm.handle(mVal, mTable, sessionId, entityManager.getModifierStore(srcI, wm.dataIndex), entityManager.getModifierStore(tarI, wm.dataIndex));
        mTable.trimToSize();
        queuedModification.add(new ModCache<>(src, tar, srcI, tarI, mTable));
    }

    private class EventImpl implements ModificationEvent {
        private final T srcRf, tarRf;
        private final StatusTable.Readonly src, tar;
        private final int[] init, values, cache;
        private boolean cancel = false;

        EventImpl(T srcRf, T tarRf, StatusTable.Readonly _src, StatusTable.Readonly _tar, int[] _value) {
            this.srcRf = srcRf;
            this.tarRf = tarRf;
            this.init = _value;
            this.values = Arrays.copyOf(_value, _value.length);
            this.cache = new int[_value.length];
            this.src = _src;
            this.tar = _tar;
        }

        @Override
        public int[] value() {
            System.arraycopy(values, 0, cache, 0, values.length);
            return cache;
        }

        @Override
        public int[] initValue() {
            return init;
        }

        @Override
        public int getSrc(int id) {
            return getVal(id, src);
        }

        @Override
        public int getTar(int id) {
            return getVal(id, tar);
        }

        private int getVal(int id, StatusTable.Readonly readonly) {
            if (id < 0 || id >= Short.MAX_VALUE) return 0;
            return readonly.get(id);
        }

        @Override
        public void modify(int id, int dis) {
            if (id >= 0 && id < init.length) {
                values[id] += dis;
            }
        }

        @Override
        public Transaction beginTransaction(boolean reserve) {
            return reserve ? HandlerList.this.beginTransaction(tarRf, srcRf) : HandlerList.this.beginTransaction(srcRf, tarRf);
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.cancel = canceled;
        }

        @Override
        public boolean isCanceled() {
            return cancel;
        }

    }

    private class TransactionImpl implements Transaction {


        private T srcE, tarE;
        private final int srcI, tarI;
        private final StatusTable.Readonly srcR, tarR;

        private IntMap<int[]> paramMap;

        private boolean usedFlag = false;
        private boolean outdated = false;
        private final int sessionCreate;
        private final boolean inBuff;

        private TransactionImpl(T src, T tar, int srcI, int tarI, int sessionCreate, boolean inBuff) {
            this.srcE = src;
            this.tarE = tar;
            this.srcI = srcI;
            this.tarI = tarI;
            this.srcR = entityManager.createReadonly(srcI);
            this.tarR = entityManager.createReadonly(tarI);
            this.sessionCreate = sessionCreate;
            this.inBuff = inBuff;
        }

        private void chkState() {
            if (usedFlag || outdated || sessionCreate != sessionId) throw new IllegalStateException();
        }

        @Override
        public Transaction modifySrc(int... args) {
            modify(srcR, args);
            return this;
        }

        @Override
        public Transaction modifyTar(int... args) {
            modify(tarR, args);
            return this;
        }

        private void modify(StatusTable.Readonly ro, int... args) {
            chkState();
            int size = args.length >>> 1;
            for (int i = 0; i < size; i++) {
                ro.modify(args[i << 1], args[i << 1 | 1]);
            }
        }

        @Override
        public Transaction setSrc(int... args) {
            set(srcR, args);
            return this;
        }

        @Override
        public Transaction setTar(int... args) {
            set(tarR, args);
            return this;
        }

        private void set(StatusTable.Readonly ro, int... args) {
            chkState();
            int size = args.length >>> 1;
            for (int i = 0; i < size; i++) {
                ro.set(args[i << 1], args[i << 1 | 1]);
            }
        }

        @Override
        public Transaction setSrc(int tarId, ToIntFunction<int[]> func, int... srcIds) {
            setFunc(func, srcR, tarId, srcIds);
            return this;
        }

        @Override
        public Transaction setTar(int tarId, ToIntFunction<int[]> func, int... srcIds) {
            setFunc(func, tarR, tarId, srcIds);
            return this;
        }

        private void setFunc(ToIntFunction<int[]> func, StatusTable.Readonly ro, int id, int[] ids) {
            chkState();
            if (func != null) {
                for (int i = 0; i < ids.length; i++) ids[i] = ro.get(ids[i]);
                ro.set(id, func.applyAsInt(ids));
            }
        }

        @Override
        public Transaction paramHandler(int id, int... args) {
            chkState();
            if (paramMap == null) paramMap = new IntMap<>();
            int size = id >= 0 && id < paramSizeMap.length ? paramSizeMap[id] : 0;
            if (size > 0) {
                if (args.length > size) {
                    args = Arrays.copyOf(args, size);
                }
                paramMap.put(id, args);
            }
            return this;
        }

        @Override
        public void modify(int id, int[] value) {
            chkState();
            if (!usedFlag && id >= 0 && id < modifierAry.length) {
                callEvent(srcE, tarE, srcI, tarI, srcR, tarR, id, value, paramMap);
            }
            usedFlag = true;
            finishAutoSession();
        }
    }

    private static class EventDelay<T> {
        T srcRf, tarRf;
        StatusTable.Readonly srcR, tarR;
        int srcI, tarI;
        int modifierId;
        int[] args;
        IntMap<int[]> paramMap;
        private EventDelay(T src, T tar, int srcI, int tarI,
                           StatusTable.Readonly srcR, StatusTable.Readonly tarR,
                           int id, int[] args, IntMap<int[]> paramMap) {
            this.srcRf = src;
            this.tarRf = tar;
            this.srcI = srcI;
            this.tarI = tarI;
            this.srcR = srcR;
            this.tarR = tarR;
            this.modifierId = id;
            this.args = args;
            this.paramMap = paramMap;
        }
    }

    private static class ModCache<T> {
        T src, tar;
        int srcI, tarI;
        ModificationTable mTable;
        private ModCache(T src, T tar, int srcI, int tarI, ModificationTable mt) {
            this.src = src;
            this.tar = tar;
            this.srcI = srcI;
            this.tarI = tarI;
            this.mTable = mt;
        }
    }

}
