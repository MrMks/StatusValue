package com.github.mrmks.utils;

public class StringIntMap extends ObjectIntMap<String> {

    public StringIntMap() {}

    public StringIntMap(int cap) {
        super(cap);
    }

    private static int hash(String key) {
        int h;
        return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private static int compare(String key, String old) {
        if (key == null && old == null) return 0;
        else if (key == null) return -1;
        else if (old == null) return 1;
        else return key.compareTo(old);
    }

    @Override
    protected int keyHash(String key) {
        return hash(key);
    }

    @Override
    protected int keyCompare(String key, String old) {
        return compare(key, old);
    }

    @Override
    protected String[] createArray(int size) {
        return new String[size];
    }

    @Override
    protected ObjectIntRoMap<String> createNonEmpty(int cap, int size, int[] cvt, int len, String[] strings, int[] vs) {
        return new Readonly(cap, size, cvt, len, strings, vs);
    }

    private static class Readonly extends ObjectIntMap.Readonly<String> {
        private Readonly(int cap, int size, int[] cvt, int len, String[] ks, int[] vs) {
            super(cap, size, cvt, len, ks, vs);
        }

        @Override
        protected int hash(String key) {
            return StringIntMap.hash(key);
        }

        @Override
        protected int compare(String key, String old) {
            return StringIntMap.compare(key, old);
        }
    }

}
