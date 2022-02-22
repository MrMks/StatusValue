package com.github.mrmks.utils;

import java.util.Arrays;

public class IntArray {

    private int[] data;

    public IntArray(int size) {
        this.data = new int[size];
    }

    public int at(int i) {
        return data[i];
    }

    public int set(int i, int v) {
        return data[i] = v;
    }

    public int indexOf(int v) {
        return indexOf(v, data.length);
    }

    public int indexOf(int v, int l) {
        l = Math.min(data.length, l);
        for (int i = 0; i < l; i++) {
            if (v == data[i]) return i;
        }
        return -1;
    }

    public int size() {
        return data.length;
    }

    public IntArray resize(int size) {
        if (size != data.length) data = Arrays.copyOf(data, size);
        return this;
    }

    public IntArray enlarge(int size) {
        if (size > 0) {
            data = Arrays.copyOf(data, data.length + size);
        }
        return this;
    }


    public int binarySearch(int v) {
        return Arrays.binarySearch(data, v);
    }

    public IntArray sort() {
        Arrays.sort(data);
        return this;
    }

    public IntArray fill(int v) {
        Arrays.fill(data, v);
        return this;
    }

    public int[] toArray() {
        return data;
    }
}
