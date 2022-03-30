package com.github.mrmks.utils;

import java.util.Arrays;
import java.util.Iterator;

public class StringIntMap extends ObjectIntMap<String> {

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
    public ObjectIntRoMap<String> readMap() {
        int len = 0;

        for (Entry<String> e : data) if (e != null) len++;

        int[] cvt = new int[(len << 1) + 1];
        cvt[len << 1] = size;

        int h = 0, c = 0;
        String[] ks = new String[size];
        int[] vs = new int[size];
        for (int i = 0; i < data.length; i++) {
            Entry<String> e = data[i];
            if (e != null) {
                cvt[h] = i;
                cvt[len + h] = c;
                h++;
                do {
                    ks[c] = e.key;
                    vs[c] = e.val;
                    c ++;
                } while ((e = e.next) != null);
            }
        }
        return new ReadOnly(cap, size, cvt, len, ks, vs);
    }

    private static class ReadOnly implements ObjectIntRoMap<String> {

        private final int cap, size, len;
        private final int[] cvt;
        private final String[] ks;
        private final int[] vs;

        private ReadOnly(int cap, int size, int[] cvt, int len, String[] ks, int[] vs) {
            this.cap = cap;
            this.size = size;
            this.cvt = cvt;
            this.len = len;
            this.ks = ks;
            this.vs = vs;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public int get(String key) {
            return getOrDefault(key, 0);
        }

        @Override
        public int getOrDefault(String key, int def) {
            int h = hash(key);
            int i = indexOf(h, cap);
            int j = Arrays.binarySearch(cvt, 0, len, i);
            if (j > 0) {
                j = Arrays.binarySearch(ks, cvt[j + len], cvt[j + len + 1], key, StringIntMap::compare);
                return j < 0 ? def : vs[j];
            }
            return def;
        }

        @Override
        public Iterator<String> keyIterator() {
            return new KeyIterator();
        }

        @Override
        public IntIterator valueIterator() {
            return new ValueIterator();
        }

        @Override
        public Iterator<ToIntEntry<String>> iterator() {
            return new EntryIterator();
        }

        private class KeyIterator implements Iterator<String> {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public String next() {
                return ks[i++];
            }
        }

        private class ValueIterator implements IntIterator {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public int next() {
                return vs[i++];
            }
        }

        private class EntryIterator implements Iterator<ToIntEntry<String>> {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public ToIntEntry<String> next() {
                return new EntryImpl(i++);
            }
        }

        private class EntryImpl implements ToIntEntry<String> {
            private final int i;
            private EntryImpl(int i) {
                this.i = i;
            }
            @Override
            public String getKey() {
                return ks[i];
            }

            @Override
            public int getValue() {
                return vs[i];
            }
        }
    }
}
