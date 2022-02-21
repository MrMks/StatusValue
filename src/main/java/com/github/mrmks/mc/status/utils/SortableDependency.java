package com.github.mrmks.mc.status.utils;

public class SortableDependency extends SimpleDependency {
    
    protected static final int BEFORE = 0;
    protected static final int AFTER = 1;

    public static SortableDependency newRequiredBefore(String key) {
        return new SortableDependency(key, REQUIRED, BEFORE);
    }

    public static SortableDependency newRequiredAfter(String key) {
        return new SortableDependency(key, REQUIRED, AFTER);
    }
    public static SortableDependency newOptionalBefore(String key) {
        return new SortableDependency(key, OPTIONAL, BEFORE);
    }

    public static SortableDependency newOptionalAfter(String key) {
        return new SortableDependency(key, OPTIONAL, AFTER);
    }

    private final int rel;
    protected SortableDependency(String key, int req, int rel) {
        super(key, req);
        this.rel = rel;
    }

    public boolean isBefore() {
        return rel == BEFORE;
    }

    public boolean isBehind() {
        return rel == AFTER;
    }
}
