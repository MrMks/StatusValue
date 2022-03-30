package com.github.mrmks.utils;

public interface IntIterator {

    boolean hasNext();

    /**
     * @throws java.util.NoSuchElementException if the iteration has no more elements
     */
    int next();

    default void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
