package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.api.BuffType;
import com.github.mrmks.mc.status.utils.IntQueue;

import java.util.Arrays;

class StatusEntity {
    final byte[] storeKey;
    final StatusTable table;
    final int[][] data;
    final int id;

    final boolean shouldSave;

    final TaskManager.Task resourceTask;
    int[] buffs;
    int buffSize;
    IntQueue refreshedBuff;

    int buffRefCount = 0;
    boolean offline = false;

    StatusEntity(byte[] bytes, StatusTable st, int[][] data, boolean shouldSave, int id, TaskManager.Task resourceTask) {
        this.storeKey = bytes;
        this.table = st;
        this.data = data;
        this.shouldSave = shouldSave;
        this.id = id;
        this.resourceTask = resourceTask;
        this.buffs = new int[16];
        Arrays.fill(buffs, -1);
        refreshedBuff = new IntQueue();
    }

    int[] getData(int dataIndex) {
        return dataIndex < 0 ? Constants.EMPTY_ARY_INT : data[dataIndex];
    }

    int assignBuffId(int id) {
        int r;
        if (refreshedBuff.isEmpty()) {
            if (buffSize >= buffs.length) {
                buffs = Arrays.copyOf(buffs, buffSize << 1);
                Arrays.fill(buffs, buffSize, buffSize << 1, -1);
            }
            r = buffSize;
            buffSize++;
        } else {
            r = refreshedBuff.remove();
        }
        buffs[r] = id;
        return r;
    }

    void freeBuffId(int id) {
        if (id >= buffSize || buffs[id] < 0) return;
        buffs[id] = -1;
        if (id + 1 == buffSize) buffSize -= 1; else refreshedBuff.offer(id);
    }

    @Deprecated
    void addBuff(BuffData data, TaskManager.Task task) {

    }

    @Deprecated
    void removeBuff(BuffType type, byte[] src, String str, boolean asKey, boolean force, boolean any) {

    }
}
