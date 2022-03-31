package com.github.mrmks.utils;

import java.util.Iterator;

public interface ObjectIntRoMap<K> {
    int size();
    boolean isEmpty();
    int getOrDefault(K key, int def);
    Iterator<K> keyIterator();
    IntIterator valueIterator();
    Iterator<ToIntEntry<K>> iterator();
}
