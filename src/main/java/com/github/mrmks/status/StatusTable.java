package com.github.mrmks.status;

import java.util.Arrays;

/**
 * This is the table contains entity's status.
 * The total size of data should equal to the number's of attributeAry in EntityManager;
 * Since this is an unchangeable size, we will use a pooled array to copy the data;
 *
 * And, this class do not export to extensions.
 */
public class StatusTable {

    private final int size;
    private final int[][] data;


    StatusTable(int _s) {
        this.size = _s;
        int sizeA = size >>> 10, sizeB = size & 1023;
        this.data = new int[sizeA + (sizeB == 0 ? 0 : 1)][];
        for (int i = 0; i < sizeA - 1; i++) {
            data[i] = new int[1024];
        }
        data[sizeA] = new int[sizeB == 0 ? 1024 : sizeB];
    }

    int get(int id) {
        if (id < 0 || id >= size) throw new IndexOutOfBoundsException();
        return data[id >> 10][id & 1023];
    }

    int accept(int id, int v) {
        if (id < 0 || id >= size) throw new IndexOutOfBoundsException();
        int i = id >> 10, j = id & 1023;
        int r = data[i][j];
        data[i][j] += v;
        return r;
    }

    int set(int id, int v) {
        if (id < 0 || id >= size) throw new IndexOutOfBoundsException();
        int i = id >> 10, j = id & 1023;
        int r = data[i][j];
        data[i][j] = v;
        return r;
    }

    void copy(int[] copied) {
        int i = 0, j = 0, l, size = copied.length;
        int[] d = data[i];
        while (j < size) {
            l = Math.min(size - j, d.length);
            System.arraycopy(d, 0, copied, j, l);
            j += l;
            ++i;
            d = data[i];
        }
    }

    Readonly readonly() {
        int[][] copied = new int[data.length][];
        for (int i = 0; i < data.length; i++) {
            copied[i] = Arrays.copyOf(data[i], data[i].length);
        }
        return new Readonly(copied, size);
    }

    Readonly readonly(int[][] copied) {
        if (copied != null) {
            for (int i = 0; i < Math.min(copied.length, data.length); i++) {
                if (copied[i] != null) {
                    System.arraycopy(data[i], 0, copied[i], 0, Math.min(copied[i].length, data[i].length));
                }
            }
        }
        return new Readonly(copied, size);
    }

    Readonly readonly(Readonly ro) {
        for (int i = 0; i < data.length; ++i) {
            System.arraycopy(data[i], 0, ro.copied[i], 0, data[i].length);
        }
        return ro;
    }

    static class Readonly {
        private final int[][] copied;
        private final int size;

        private Readonly(int[][] copied, int size) {
            this.copied = copied;
            this.size = size;
        }

        public int get(int id) {
            return id >= 0 && id < size ? copied[id >> 10][id & 0x3ff] : 0;
        }

        void modify(int id, int val) {
            if (id >= 0 && id < size) copied[id >> 10][id & 0x3ff] += val;
        }

        void set(int id, int val) {
            if (id >= 0 && id < size) copied[id >> 10][id & 0x3ff] = val;
        }
    }
}
