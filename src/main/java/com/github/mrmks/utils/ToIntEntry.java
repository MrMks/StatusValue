package com.github.mrmks.utils;

public interface ToIntEntry<K> {
    K getKey();
    int getValue();
    default int setValue(int value) {
        throw new UnsupportedOperationException("setValue");
    }
    boolean equals(Object o);
    int hashCode();
}
