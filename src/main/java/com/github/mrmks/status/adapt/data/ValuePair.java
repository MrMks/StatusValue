package com.github.mrmks.status.adapt.data;

public class ValuePair {
    private final String key;
    private final int value;
    public ValuePair(String k, int v) {
        if (k == null || k.isEmpty()) throw new IllegalArgumentException("illegal resource key");
        this.key = k;
        this.value = v;
    }

    public String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }
}
