package com.github.mrmks.mc.status.utils;

import java.util.Arrays;

public class ShortArray {
    private short[] data;

    public ShortArray(int size) {
        this.data = new short[size];
    }

    public short at(int i) {
        return data[i];
    }

    public short set(int i, short v) {
        return data[i] = v;
    }

    public int indexOf(short v) {
        return indexOf(v, data.length);
    }

    public int indexOf(short v, int l) {
        l = Math.min(data.length, l);
        for (short i = 0; i < l; i++) {
            if (v == data[i]) return i;
        }
        return -1;
    }

    public int size() {
        return data.length;
    }

    public ShortArray resize(int size) {
        if (size != data.length) data = Arrays.copyOf(data, size);
        return this;
    }

    public ShortArray enlarge(int size) {
        if (size > 0) {
            data = Arrays.copyOf(data, data.length + size);
        }
        return this;
    }

    public int binarySearch(short v) {
        return Arrays.binarySearch(data, v);
    }

    public ShortArray sort() {
        Arrays.sort(data);
        return this;
    }

    public ShortArray fill(short v) {
        Arrays.fill(data, v);
        return this;
    }

}
