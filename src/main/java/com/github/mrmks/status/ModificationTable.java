package com.github.mrmks.status;

import com.github.mrmks.status.api.ModificationCache;
import com.github.mrmks.status.api.BuffType;
import com.github.mrmks.utils.IntArray;

import java.util.LinkedList;

/**
 * This is a table contains two StatusTable.Readonly and contains two array pair to log the modification of to status
 */

class ModificationTable implements ModificationCache {

    private final int size;
    private final StatusTable.Readonly src, tar;
    final IntArray srcId, tarId;
    final IntArray srcVal, tarVal;
    private int srcLe, tarLe, srcLa, tarLa, srcLai, tarLai;
    final LinkedList<BuffCache> buffCaches;

    private final boolean srcSystem;

    ModificationTable(StatusTable.Readonly src, StatusTable.Readonly tar, int size, boolean srcSystem) {
        this.src = src;
        this.tar = tar;
        this.srcId = new IntArray(8);
        this.tarId = new IntArray(8);
        this.srcVal = new IntArray(8);
        this.tarVal = new IntArray(8);
        this.size = size;
        this.srcSystem = srcSystem;
        this.buffCaches = new LinkedList<>();
    }

    @Override
    public boolean isSrcSystem() {
        return srcSystem;
    }

    @Override
    public int getSrc(int id) {
        return src == null ? 0 : src.get(id);
    }

    @Override
    public int getTar(int id) {
        return tar == null ? 0 : tar.get(id);
    }

    @Override
    public void modifySrc(int id, int val) {
        int i = modify(id, val, srcLa, srcLai, srcLe, srcId, srcVal);
        if (i >= 0) {
            srcLa = id;
            srcLai = i;
            srcLe += srcLe == i ? 1 : 0;
        }
    }

    @Override
    public void modifyTar(int id, int val) {
        int i = modify(id, val, tarLa, tarLai, tarLe, tarId, tarVal);
        if (i >= 0) {
            tarLa = id;
            tarLai = i;
            tarLe += i == tarLe ? 1 : 0;
        }
    }

    @Override
    public void buffAttribute(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int duration, boolean anySource, boolean canRemove, boolean reverse) {
        buffCaches.add(new BuffCache(key, tag, icon, type, idTar, valTar, idSrc, valSrc, duration, anySource, canRemove, reverse));
    }

    @Override
    public void buffResource(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int interval, int count, boolean anySource, boolean canRemove, boolean reverse) {
        buffCaches.add(new BuffCache(key, tag, icon, type, idTar, valTar, idSrc, valSrc, interval, count, anySource, canRemove, false, reverse));
    }

    @Override
    public void buffResource(String key, String tag, String icon, BuffType type, int id, int[] val, int interval, int count, boolean anySource, boolean canRemove, boolean reverse) {
        buffCaches.add(new BuffCache(key, tag, icon, type, new int[]{id}, val, null, null, interval, count, anySource, canRemove, false, reverse));
    }

    @Override
    public void removeBuffSrc(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force) {
        buffCaches.add(new BuffCache(type, key, tag, anySource, once, force, true));
    }

    @Override
    public void removeBuffTar(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force) {
        buffCaches.add(new BuffCache(type, key, tag, anySource, once, force, false));
    }

    private int modify(int id, int val, int la, int lai, int le, IntArray ids, IntArray vs) {
        if (id < 0 || id >= size) return -1;
        if (id == la) {
            vs.set(lai, vs.at(lai) + val);
            return lai;
        } else {
            int index = vs.indexOf(id, le);
            if (index < 0) {
                if (le == ids.size()) {
                    ids.resize(le << 1);
                    vs.enlarge(le << 1);
                }
                ids.set(le, id);
                vs.set(index = le, val);
            } else vs.set(index, vs.at(index) + val);
            return index;
        }
    }

    void trimToSize() {
        srcId.resize(srcLe);
        srcVal.resize(srcLe);
        tarId.resize(tarLe);
        tarVal.resize(tarLe);
    }

    static class BuffCache {
        // if bool[0] is true, this means attaching some buff to someone. the next 5 bool means reverse, anySource, toAttribute, canRemove and asSource.
        // if else, this means removing some buff from someone. the next 4 bool means toSrc, anySource, once, force;
        final boolean adding, reverse, anySource, attributeOrOnce, canRemoveOrForce, asSource;
        final BuffType type;
        final String key, tag, icon;
        final int[] idTar, valTar, idSrc, valSrc;
        // args only used in adding conditions, contains [interval, count] or [duration].
        final int[] args;

        private BuffCache(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int duration, boolean anySource, boolean canRemove, boolean reverse) {
            this.adding = true;
            this.reverse = reverse;
            this.anySource = anySource;
            this.attributeOrOnce = true;
            this.canRemoveOrForce = canRemove;
            this.asSource = false;
            this.type = type;
            this.key = key;
            this.tag = tag;
            this.icon = icon;
            this.idTar = idTar;
            this.valTar = valTar;
            this.idSrc = idSrc;
            this.valSrc = valSrc;
            this.args = new int[]{duration};
        }

        private BuffCache(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int interval, int count, boolean anySource, boolean canRemove, boolean source, boolean reverse) {
            this.adding = true;
            this.reverse = reverse;
            this.anySource = anySource;
            this.attributeOrOnce = false;
            this.canRemoveOrForce = canRemove;
            this.asSource = source;
            this.type = type;
            this.key = key;
            this.tag = tag;
            this.icon = icon;
            this.idTar = idTar;
            this.valTar = valTar;
            this.idSrc = idSrc;
            this.valSrc = valSrc;
            this.args = new int[] {interval, count};
        }

        private BuffCache(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force, boolean toSrc) {
            this.adding = false;
            this.reverse = toSrc;
            this.anySource = anySource;
            this.attributeOrOnce = once;
            this.canRemoveOrForce = force;
            this.asSource = false;
            this.type = type;
            this.key = key;
            this.tag = tag;
            this.icon = null;
            this.idTar = this.idSrc = this.valTar = this.valSrc = this.args = null;
        }
    }

}
