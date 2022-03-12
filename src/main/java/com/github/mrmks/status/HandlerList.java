package com.github.mrmks.status;

import com.github.mrmks.status.api.ModificationEvent;
import com.github.mrmks.utils.CommonUtils;
import com.github.mrmks.utils.IntMap;

import java.util.*;
import java.util.function.ToIntFunction;

public class HandlerList<T> {

    private final WrappedModifier[] modifierAry;
    private final IntMap<WrappedHandler[]> handlerAry;
    private final HashMap<String, WrappedHandler> handlerMap;
    private final int resourceSize;

    private final int[] paramSizeMap;

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

    /**
     * For system to do modifications on entity
     */
    Transaction beginTransaction(T tar) {
        if (!sessionControl.isInSession()) clearQueue();
        if (!sessionControl.beginAutoSession()) throw new IllegalStateException();

        return beginTransaction0(null, tar);
    }

    Transaction beginTransaction(T src, T tar) {
        if (!sessionControl.isInSession()) clearQueue();
        if (!sessionControl.beginAutoSession()) throw new IllegalStateException();

        return beginTransaction0(src, tar);
    }

    @Deprecated
    Transaction beginTransaction(int src, int tar, T srcE, T tarE) {
        if (!sessionControl.isInSession()) clearQueue();
        if (!sessionControl.beginAutoSession()) throw new IllegalStateException();

        TransactionImpl trImpl = new TransactionImpl(src, tar, srcE, tarE, sessionId);
        queuedTransactions.add(trImpl);
        return trImpl;
    }

    private Transaction beginTransaction0(T src, T tar) {
        int srcI, tarI;
        if (src == null) {
            srcI = 0;
            if (tar == null) throw new IllegalArgumentException("Both entity instance are null");
            else {
                tarI = entityManager.findEntityIndex(tar);
                if (tarI <= 0) throw new IllegalArgumentException("Unregistered transaction target entity instance");
            }
        } else {
            srcI = entityManager.findEntityIndex(src);
            if (srcI <= 0) throw new IllegalArgumentException("Unregistered transaction source entity instance.");
            else {
                if (tar == null || tar == src) {
                    tarI = srcI;
                    tar = src;
                    src = null;
                } else {
                    tarI = entityManager.findEntityIndex(tar);
                    if (tarI <= 0) throw new IllegalArgumentException("Unregistered transaction target entity instance");
                }
            }
        }
        TransactionImpl trImpl = new TransactionImpl(srcI, tarI, src, tar, sessionId);
        queuedTransactions.add(trImpl);
        return trImpl;
    }

    /*
     * This method should only be invoked by ModificationEvent#beginTransaction, where the valid check has been performed
     * Means this method will not perform any valid check at all;
     */
    private Transaction directTransaction(int src, int tar, T srcE, T tarE) {
        TransactionImpl trImpl = new TransactionImpl(src, tar, srcE, tarE, sessionId);
        queuedTransactions.add(trImpl);
        return trImpl;
    }

    void finishAutoSession() {
        if (sessionControl.canFinishAutoSession()) finishSession();
    }

    void finishSession() {
        if (sessionControl.canFinishSession()) {
            Iterator<ModCache<T>> iterator = queuedModification.iterator();
            while (iterator.hasNext()) {
                ModCache<T> entry = iterator.next();
                iterator.remove();

                int srcI = entry.srcI, tarI = entry.tarI;
                boolean selfMod = srcI == tarI;

                T src = entry.src, tar = entry.tar;

                if (tarI == 0 || !entityManager.testEntity(tar, tarI) || (!selfMod && !entityManager.testEntity(src, srcI))) {
                    continue;
                }

                ModificationTable mt = entry.mTable;
                sessionControl.applyBuff();
                Iterator<ModificationTable.BuffCache> itBuffs = entry.mTable.buffCaches.iterator();
                while (itBuffs.hasNext()) {
                    ModificationTable.BuffCache bc = itBuffs.next();
                    itBuffs.remove();

                    if (bc.reverse) {
                        if (srcI == 0) continue;
                        else if (!selfMod) {
                            src = tar; tar = entry.src;
                            srcI = tarI; tarI = entry.srcI;
                        }
                    }

                    BuffData data = new BuffData(bc.type, bc.key, bc.tag, bc.icon, bc.canRemoveOrForce, bc.anySource);
                    if (bc.adding) {
                        // add buff
                        if (bc.args == null || bc.idTar == null) continue;
                        if (bc.attributeOrOnce) {
                            // attribute buff
                            sessionControl.applyAttribute();
                            entityManager.applyAttribute(tar, tarI, bc.idTar, bc.valTar);
                            if (srcI != 0 && !selfMod) entityManager.applyAttribute(src, srcI, bc.idSrc, bc.valSrc);
                            sessionControl.applyBuff();
                            buffManager.addBuffAttribute(srcI, tarI, data,
                                    src, tar, bc.args[0], bc.idSrc, bc.valSrc, bc.idTar, bc.valTar);
                        } else {
                            if (bc.asSource) {
                                // modifier buff
                                buffManager.addBuffModifier(srcI, tarI, data,
                                        src, tar, bc.args[0], bc.args[1], bc.idTar[0], bc.valTar);
                            } else {
                                // resource buff
                                buffManager.addBuffResource(srcI, tarI, data,
                                        src, tar, bc.args[0], bc.args[1], bc.idSrc, bc.valSrc, bc.idTar, bc.valTar);
                            }
                        }
                    } else {
                        // remove buff
                        buffManager.removeBuff(tarI, srcI, data, bc.attributeOrOnce);
                    }
                }
                sessionControl.applyAttribute();
                entityManager.applyResource(tar, tarI, mt.tarId.toArray(), mt.tarVal.toArray());
                if (srcI != 0 && !selfMod && mt.srcId != null && mt.srcVal != null)
                    entityManager.applyResource(src, srcI, mt.srcId.toArray(), mt.srcVal.toArray());
            }

            clearQueue();
            sessionId ++;
            sessionControl.finishSession();
        }
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

    void callEvent(int srcI, int tarI, T src, T tar, StatusTable.Readonly srcR, StatusTable.Readonly tarR,
                           int mId, int[] mVal, IntMap<int[]> hParams) {
        if (sessionControl.canCallEvent()) {
            if (sessionControl.canCallEventDirect()) {
                callEvent0(srcI, tarI, src, tar, srcR, tarR, mId, mVal, hParams);
                while (!queuedEvents.isEmpty()) {
                    EventDelay<T> e = queuedEvents.remove();
                    callEvent0(e.srcI, e.tarI, e.srcRf, e.tarRf, e.srcR, e.tarR, e.modifierId, e.args, e.paramMap);
                }
            } else {
                queuedEvents.add(new EventDelay<>(src, tar, srcI, tarI, srcR, tarR, mId, mVal, hParams));
            }
        }
    }

    boolean isModifierValid(int id) {
        return id >= 0 && id < modifierAry.length;
    }

    private void callEvent0(int srcI, int tarI, T src, T tar, StatusTable.Readonly srcR, StatusTable.Readonly tarR,
                            int mId, int[] mVal, IntMap<int[]> hParams) {
        sessionControl.beginHandler();
        WrappedHandler[] whs = handlerAry.get(mId);
        if (whs != null && whs.length > 0) {
            short msId = (short) mId;
            EventImpl mEvent = new EventImpl(srcI, tarI, src, tar, srcR, tarR, mVal);
            for (WrappedHandler wh : whs) {
                if (wh.handleCanceled || !mEvent.cancel)
                    wh.handle(msId, mEvent, hParams == null ? Constants.EMPTY_ARY_INT : hParams.getOrDefault(wh.paramIndex, Constants.EMPTY_ARY_INT));
            }
        }
        sessionControl.beginModifier();
        ModificationTable mTable = new ModificationTable(srcR, tarR, resourceSize, srcI == 0, srcI == tarI);
        WrappedModifier wm = modifierAry[mId];
        wm.handle(mVal, mTable, sessionId, entityManager.getModifierStore(srcI, wm.dataIndex), srcI == tarI ? Constants.EMPTY_ARY_INT : entityManager.getModifierStore(tarI, wm.dataIndex));
        mTable.trimToSize();
        queuedModification.add(new ModCache<>(src, tar, srcI, tarI, mTable));
        sessionControl.finishEventCall();
    }

    private class TransactionImpl implements Transaction {

        private T srcE, tarE;
        private final int srcI, tarI;
        private final StatusTable.Readonly srcR, tarR;
        private final boolean selfModify;

        private IntMap<int[]> paramMap;

        private boolean usedFlag = false;
        private boolean outdated = false;
        private final int sessionCreate;

        private TransactionImpl(int srcI, int tarI, T src, T tar, int sessionCreate) {
            this.srcI = srcI;
            this.tarI = tarI;
            this.selfModify = srcI == tarI;
            this.tarE = tar;
            this.tarR = entityManager.createReadonly(tarI);
            if (selfModify) {
                this.srcE = null;
                this.srcR = null;
            } else {
                this.srcE = src;
                this.srcR = entityManager.createReadonly(srcI);
            }
            this.sessionCreate = sessionCreate;
        }

        private void chkState() {
            if (usedFlag || outdated || sessionCreate != sessionId) throw new IllegalStateException();
        }

        @Override
        public Transaction modifySrc(int... args) {
            modify(selfModify ? tarR : srcR, args);
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
            set(selfModify ? tarR : srcR, args);
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
            setFunc(func, selfModify ? tarR : srcR, tarId, srcIds);
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
                args = Arrays.copyOf(args, size);
                paramMap.put(id, args);
            }
            return this;
        }

        @Override
        public void modify(int id, int[] value) {
            chkState();
            usedFlag = true;
            if (isModifierValid(id)) {
                callEvent(srcI, tarI, srcE, tarE, srcR, tarR, id, value == null ? Constants.EMPTY_ARY_INT : value, paramMap);
                finishAutoSession();
            } else {
                throw new IllegalArgumentException("The modifier id is not valid: ".concat(Integer.toString(id)));
            }
        }
    }

    private class EventImpl implements ModificationEvent {
        private final T srcRf, tarRf;
        private final int srcI, tarI;
        private final StatusTable.Readonly src, tar;
        private final int[] init, initRead, values, cache;
        private final boolean selfMod, systemMod;
        private boolean cancel = false;

        EventImpl(int srcI, int tarI, T srcRf, T tarRf, StatusTable.Readonly _src, StatusTable.Readonly _tar, int[] _value) {
            this.srcI = srcI;
            this.tarI = tarI;
            this.selfMod = srcI == tarI;
            this.systemMod = srcI == 0;
            this.init = _value;
            this.initRead = new int[init.length];
            this.srcRf = srcRf;
            this.tarRf = tarRf;
            this.values = Arrays.copyOf(_value, _value.length);
            this.cache = new int[_value.length];
            this.src = _src;
            this.tar = _tar;
        }

        @Override
        public boolean isSrcSystem() {
            return systemMod;
        }

        @Override
        public boolean isSelfModify() {
            return selfMod;
        }

        @Override
        public int[] value() {
            System.arraycopy(values, 0, cache, 0, values.length);
            return cache;
        }

        @Override
        public int[] initValue() {
            System.arraycopy(init, 0, initRead, 0, init.length);
            return initRead;
        }

        @Override
        public int getSrc(int id) {
            return getVal(id, selfMod ? tar : src);
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
            if (reserve && srcI == 0) throw new IllegalArgumentException("You can nor begin transaction with reserve is true while the #isSrcSystem returns true");
            return reserve && srcI != tarI ? directTransaction(tarI, srcI, tarRf, srcRf) : directTransaction(srcI, tarI, srcRf, tarRf);
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
