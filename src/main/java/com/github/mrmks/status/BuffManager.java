package com.github.mrmks.status;

import com.github.mrmks.status.adapt.IGuiCallback;
import com.github.mrmks.status.api.BuffType;
import com.github.mrmks.utils.IntQueue;

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
    private final StateControl sessionControl;

    private BuffTask[][] buffAry;
    private int size = 0;
    private final IntQueue released = new IntQueue();
    private final int cap = Constants.DEFAULT_ARRAY_INIT_CAPACITY_FACTOR;
    private final int maximumTickLimit = Constants.TICKS_PER_SEC * 3600;

    BuffManager(
            TaskManager taskManager,
            IGuiCallback guiCallback,
            ConfigMap configMap,
            StateControl sessionControl
    ) {
        this.taskManager = taskManager;
        this.guiCallback = guiCallback;
        this.GUI_UPDATE_INTERVAL = configMap.getGuiInterval() * Constants.TICKS_PER_SEC;
        this.sessionControl = sessionControl;
    }

    void setup(EntityManager<T> e, HandlerList<T> l) {
        if (handlerList == null && entityManager == null) {
            handlerList = l;
            entityManager = e;
        }
    }

    // handled resource
    void addBuffModifier(int srcI, int tarI, BuffData data, T srcE, T tarE, int interval, int count, int id, int[] val) {
        if (interval < 0 || count == 0 || !handlerList.isModifierValid(id)) return;

        boolean selfMod = srcI == tarI;
        StatusEntity tar = entityManager.getEntity0(tarI);
        StatusEntity src = selfMod ? tar : entityManager.getEntity0(srcI);

        if (count < 0 || count > maximumTickLimit || count + interval * interval >= maximumTickLimit) count = -1;
        else if (srcI > 0) src.buffRefCount++;

        BuffTask task = matchBuff(tar, data, src.storeKey);

        if (task != null) {
            if (count > 0) tar.buffRefCount++;
            int l = val == null ? 0 : val.length;
            int[] args = new int[2 + l];
            args[0] = id;
            args[1] = l;
            if (l > 0) System.arraycopy(val, 0, args, 2, l);

            expandTask(task, data, srcE, src.storeKey, srcI, interval, count, args);
        } else {
            int bid = nextId();
            int ebid = tar.assignBuffId(bid);

            task = new ModifierBuff(tarI, ebid, srcI, tar.storeKey, src.storeKey, data, interval, count, tarE, srcE, id, val == null ? Constants.EMPTY_ARY_INT : val);

            addBuff0(bid, task);
        }
    }

    void addBuffResource(int srcI, int tarI, BuffData data, T srcE, T tarE, int interval, int count, int[] srcId, int[] srcVal, int[] tarId, int[] tarVal) {
        if (tarI == 0 || interval < 0 || count == 0 ) return;
        boolean selfMod = srcI == tarI;
        int sizeSrc = selfMod ? 0 : checkIdAndVal(srcId, srcVal, true);
        int sizeTar = checkIdAndVal(tarId, tarVal, true);

        if ((sizeSrc ^ sizeTar) == 0) return;

        StatusEntity tar = entityManager.getEntity0(tarI);
        StatusEntity src = selfMod ? tar : entityManager.getEntity0(srcI);

        if (count < 0 || count >= maximumTickLimit || count + interval * count >= maximumTickLimit) count = -1;
        else if (srcI > 0) src.buffRefCount++;

        BuffTask task = matchBuff(tar, data, src.storeKey);
        if (task != null) {
            if (count > 0) tar.buffRefCount++;
            int[] args = new int[sizeSrc + sizeTar + 2];
            args[0] = sizeSrc;
            args[1] = sizeTar;

            if (sizeSrc > 0) {
                System.arraycopy(srcId, 0, args, 2, sizeSrc);
                System.arraycopy(srcVal, 0, args, 2 + sizeSrc, sizeSrc);
            }
            if (sizeTar > 0) {
                System.arraycopy(tarId, 0, args, 2 + (sizeSrc << 1), sizeTar);
                System.arraycopy(tarVal, 0, args, 2 + (sizeSrc << 1) + sizeTar, sizeTar);
            }

            expandTask(task, data, srcE, src.storeKey, srcI, interval, count, args);
        } else {
            if (sizeSrc == 0 || srcI == 0) srcId = srcVal = Constants.EMPTY_ARY_INT;
            else if (sizeSrc != srcId.length) {
                srcId = Arrays.copyOfRange(srcId, 0, sizeSrc);
                srcVal = Arrays.copyOfRange(srcVal, 0, sizeSrc);
            }

            if (sizeTar == 0) tarId = tarVal = Constants.EMPTY_ARY_INT;
            else if (sizeTar != tarId.length) {
                tarId = Arrays.copyOfRange(tarId, 0, sizeTar);
                tarVal = Arrays.copyOfRange(tarVal, 0, sizeTar);
            }

            int bid = nextId();
            int ebid = tar.assignBuffId(bid);

            task = new ResourceBuff(tarI, ebid, srcI, tar.storeKey, src.storeKey, data,
                    interval, count, tarE, srcE, srcId, tarId, srcVal, tarVal);

            addBuff0(bid, task);
        }
    }

    void addBuffAttribute(int srcI, int tarI, BuffData data, T srcE, T tarE, int duration, int[] srcId, int[] srcVal, int[] tarId, int[] tarVal) {
        if (duration == 0 || tarI == 0) return;
        boolean selfMod = srcI == tarI;
        int sizeSrc = selfMod ? 0 : checkIdAndVal(srcId, srcVal, false);
        int sizeTar = checkIdAndVal(tarId, tarVal, false);

        if ((sizeSrc ^ sizeTar) == 0) return;

        StatusEntity tar = entityManager.getEntity0(tarI);
        StatusEntity src = selfMod ? tar : entityManager.getEntity0(srcI);

        if (duration < 0 || duration >= maximumTickLimit) duration = -1;
        else if (srcI > 0 && !selfMod) src.buffRefCount++;

        BuffTask task = matchBuff(tar, data, src.storeKey);

        int count = duration < 0 ? -1 : 2;
        int interval = duration < 0 ? Integer.MAX_VALUE : duration;

        if (task != null) {
            if(count > 0) tar.buffRefCount++;
            int[] args = new int[sizeSrc + sizeTar + 2];
            args[0] = sizeSrc;
            args[1] = sizeTar;

            if (sizeSrc > 0) {
                System.arraycopy(srcId, 0, args, 2, sizeSrc);
                System.arraycopy(srcVal, 0, args, 2 + sizeSrc, sizeSrc);
            }
            if (sizeTar > 0) {
                System.arraycopy(tarId, 0, args, 2 + (sizeSrc << 1), sizeTar);
                System.arraycopy(tarVal, 0, args, 2 + (sizeSrc << 1) + sizeTar, sizeTar);
            }

            expandTask(task, data, srcE, src.storeKey, srcI, interval, count, args);
        } else {

            if (sizeSrc == 0 || srcI == 0) srcId = srcVal = Constants.EMPTY_ARY_INT;
            else if (sizeSrc != srcId.length) {
                srcId = Arrays.copyOfRange(srcId, 0, sizeSrc);
                srcVal = Arrays.copyOfRange(srcVal, 0, sizeSrc);
            }

            if (sizeTar == 0) tarId = tarVal = Constants.EMPTY_ARY_INT;
            else if (sizeTar != tarId.length) {
                tarId = Arrays.copyOfRange(tarId, 0, sizeTar);
                tarVal = Arrays.copyOfRange(tarVal, 0, sizeTar);
            }

            int bid = nextId();
            int ebid = tar.assignBuffId(bid);

            task = new AttributeBuff(tarI, ebid, srcI, tar.storeKey, src.storeKey, data, interval, count, tarE, srcE,
                    tarId, tarVal, srcId, srcVal);

            addBuff0(bid, task);
        }
    }

    private int checkIdAndVal(int[] idAry, int[] valAry, boolean resource) {
        if (idAry == null || valAry == null || idAry.length != valAry.length) return 0;

        int i = 0, j = 0;
        for (; i < idAry.length; ++i) {
            int id = idAry[i], val = valAry[i];
            if (!(resource ? entityManager.isResourceId(id) : entityManager.isAttributeId(id)) || val == 0) {
                for (; j < idAry.length; ++j) {
                    id = idAry[i];
                    val = valAry[i];
                    if ((resource ? entityManager.isResourceId(id) : entityManager.isAttributeId(id)) && val != 0) break;
                }
                if (j < idAry.length) {
                    idAry[i] = id;
                    valAry[i] = val;
                } else break;
            }
        }
        return i;
    }

    @SuppressWarnings("unchecked")
    private void addBuff0(int bid, BuffTask task) {
        if (buffAry == null) {
            buffAry = new BuffManager.BuffTask[1][];
            buffAry[0] = new BuffManager.BuffTask[1 << cap];
        }
        int i = bid >>> cap, j = bid & (1 << cap) - 1;
        if (i == buffAry.length) {
            buffAry = Arrays.copyOf(buffAry, i << 1);
            buffAry[i] = new BuffManager.BuffTask[1 << cap];
        }
        buffAry[i][j] = task;
        taskManager.addTask(task);
    }

    void removeBuff(int srcI, int tarI, BuffData data, boolean once) {
        if (data == null) return;
        StatusEntity src = entityManager.getEntity(srcI);
        StatusEntity tar = entityManager.getEntity(tarI);

        if (tar == null) return;

        int[] buffs = tar.buffs;
        for (int count = 0; count < buffs.length; ++count) {
            int id = buffs[count];
            BuffTask bt = getBuff(id, tarI, count);
            if (bt != null) {
                boolean isMatch = (data.type == null || bt.type == null || data.type == bt.type)
                        && (data.key == null || data.key.equals(bt.key))
                        && (data.tag == null || data.tag.equals(bt.tag))
                        && (data.anySrc || src == null || bt.src == null || Arrays.equals(src.storeKey, bt.src))
                        && (data.canRemove || bt.canRemove);
                if (isMatch) {
                    bt.cancel(true);
                    bt.onRemove();
                    emptyBuff(id);
                    if (once) break;
                }
            }
        }
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
        if (id + 1 == size) size--; else released.offer(id);
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
                if (guiCallback != null)
                    guiCallback.updateBuff(tar, key, src, anySource, icon, logicInterval * (logicCount - 1) + logicRemaining);
            }
            return Math.min(guiRemaining, logicRemaining);
        }

        abstract void runLogic();

        @Override
        final void onRemove() {
            if (logicCount >= 0) {
                entityManager.reduceEntityRef(eidSrc);
                entityManager.reduceEntityRef(eidTar);
            }
            entityManager.getEntity(eidTar).freeBuffId(ebidTar);
            if (guiCallback != null) guiCallback.removeBuff(tar, key, src, anySource);
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

    private class ResourceBuff extends BuffTask {

        private final WeakReference<T> tarE;
        private WeakReference<T> srcE;
        private int[] srcId, tarId, srcVal, tarVal;

        protected ResourceBuff(int eid, int ebid, int eidSrc, byte[] tar, byte[] src, BuffData data, int interval, int count,
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
            if (src != null && srcId != null && srcVal != null && srcId.length > 0) {
                entityManager.applyResource(src, eidSrc, srcId, srcVal);
            }
            if (tar != null && tarId != null && tarVal != null && tarId.length > 0) {
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

    private class ModifierBuff extends BuffTask {
        private final WeakReference<T> tar;
        private WeakReference<T> src;
        private int id;
        private int[] val;
        protected ModifierBuff(int eid, int ebid, int eidSrc, byte[] tar, byte[] src, BuffData data, int interval, int count,
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
            StatusEntity se = entityManager.getEntity(eidTar);
            if (se == null) {
                cancel(true);
                return;
            }
            if (eidSrc != 0) {
                se = entityManager.getEntity(eidSrc);
                if (se == null) {
                    cancel(true);
                    return;
                }
            }

            boolean selfMod = eidSrc == eidTar;
            sessionControl.beginSession();
            handlerList.callEvent(eidSrc, eidTar, src, tar, selfMod ? null : entityManager.createReadonly(eidSrc), entityManager.createReadonly(eidTar), id, val, null);
            sessionControl.finishSession();
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
                if (tarId != null && tarId.length > 0) {
                    entityManager.applyAttribute(tar.get(), eidTar, tarId, tarVal);
                }
                if (srcId != null && srcId.length > 0) {
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
