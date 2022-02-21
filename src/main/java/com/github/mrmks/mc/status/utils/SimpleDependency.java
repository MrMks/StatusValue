package com.github.mrmks.mc.status.utils;

public class SimpleDependency {

    protected static final int REQUIRED = 0;
    protected static final int OPTIONAL = 1;

    public static SimpleDependency newRequired(String key) {
        return new SimpleDependency(key, REQUIRED);
    }

    public static SimpleDependency newOptional(String key) {
        return new SimpleDependency(key, OPTIONAL);
    }

    private final String key;
    private final int req;
    protected SimpleDependency(String key, int req) {
        this.key = key;
        this.req = req;
    }

    public String getKey() {
        return key;
    }

    public boolean isRequired() {
        return req == REQUIRED;
    }

    public boolean isOptional() {
        return req == OPTIONAL;
    }
}
