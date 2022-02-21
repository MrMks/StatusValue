package com.github.mrmks.mc.status.utils;

import java.util.NoSuchElementException;

public class IntQueue {
    private final int initCap;

    private int ri = 0, wi = 0;
    private int size;
    private int[] data;

    public IntQueue() {
        initCap = 16;
    }

    public IntQueue(int initCap) {
        this.initCap = Math.max(initCap, 16);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void offer(int val) {
        if (data == null) {
            data = new int[initCap];
            data[0] = val;
            ri = 0;
            wi = 1;
            size = 0;
        } else if (data.length == size) {
            int[] tmp = new int[data.length << 1];
            if (wi < ri) {
                System.arraycopy(data, ri, tmp, 0, data.length - ri);
                System.arraycopy(data, 0, tmp, data.length - ri, wi < 0 ? ri : wi);
            } else {
                System.arraycopy(data, ri, tmp, 0, ri - wi);
            }
            data = tmp;
            ri  = 0;
            wi = size;
            data[wi ++] = val;
        } else {
            if (wi < data.length) {
                data[wi++] = val;
            }
            if (wi == data.length) wi = 0;
            if (wi == ri) wi = -1; // in this condition, the size should equal to data.length;
        }
        size++;
    }

    public int remove() {
        if (ri == wi) {
            throw new NoSuchElementException();
        } else {
            int r;
            if (ri < wi) {
                size--;
                r = data[ri++];
            } else if (wi < 0) {
                size --;
                wi = ri;
                r = data[ri++];
            } else {
                size--;
                r = data[ri++];
                if (ri == data.length) ri = 0;
            }
            return r;
        }
    }

    public void clear() {
        ri = wi = size = 0;
    }
}
