package com.github.mrmks.status.adapt.data;

import com.github.mrmks.status.Constants;

public class StorePair {
    private final String key;
    private final int[] store;
    public StorePair(String k, int[] v) {
        if (k == null || k.isEmpty()) throw new IllegalArgumentException("illegal modifier key");
        if (v == null || v.length == 0) v = Constants.EMPTY_ARY_INT;
        this.key = k;
        this.store = v;
    }

    public String getKey() {
        return key;
    }

    public int[] getStore() {
        return store;
    }
}
