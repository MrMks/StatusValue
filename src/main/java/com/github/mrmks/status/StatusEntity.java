package com.github.mrmks.status;

import com.github.mrmks.utils.IntQueue;

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
        if (id != 0 && dataIndex >= 0 && dataIndex < data.length) {
            int[] re = data[dataIndex];
            if (re != null) return re;
        }
        return Constants.EMPTY_ARY_INT;
    }

    int assignBuffId(int id) {
        if (this.id == 0) throw new RuntimeException("Can't assign buff id for system entity");
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
        if (this.id == 0) throw new RuntimeException("Can't free buff id for system entity");
        if (id >= buffSize || buffs[id] < 0) return;
        buffs[id] = -1;
        if (id + 1 == buffSize) buffSize -= 1; else refreshedBuff.offer(id);
    }

}
