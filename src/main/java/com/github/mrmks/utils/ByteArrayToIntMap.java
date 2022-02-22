package com.github.mrmks.utils;

import java.util.Arrays;

public class ByteArrayToIntMap {
    private Entry[] data = null;
    private int size = 0;
    private int capacity;
    private int linkCount = 0;

    public ByteArrayToIntMap() {
        capacity = 16;
    }

    public ByteArrayToIntMap(int initCapacity) {
        capacity = 16;
        initCapacity = Math.min(initCapacity, 0x40000000);
        while (capacity < initCapacity) capacity <<= 1;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int put(byte[] key, int val) {
        if (data == null) data = new Entry[capacity];
        int index = indexOf(hash(key), capacity);
        if (data[index] == null) {
            size += 1;
            data[index] = new Entry(key, val);
        } else {
            Entry prev = data[index];
            do {
                if (Arrays.equals(prev.key, key)) {
                    int r = prev.val;
                    prev.val = val;
                    return r;
                }
            } while ((prev = prev.next) != null);

            prev = new Entry(key, val);
            prev.next = data[index];
            data[index] = prev;
            linkCount ++;
            size ++;

            if (size > capacity && linkCount > (capacity >> 2) && capacity < 0x40000000) {
                capacity <<= 1;
                Entry[] tar = new Entry[capacity];
                linkCount = transfer(data, tar, capacity);
                data = tar;
            }
        }
        return 0;
    }

    public boolean containKey(byte[] key) {
        if (data == null) return false;
        int i = indexOf(hash(key), capacity);
        Entry e = data[i];
        while (e != null) {
            if (Arrays.equals(e.key, key)) {
                return true;
            }
            e = e.next;
        }
        return false;
    }

    private static int transfer(Entry[] src, Entry[] tar, int cap) {
        int count = 0;
        Entry n;
        for (Entry e : src) {
            if (e == null) continue;
            do {
                n = e.next;
                e.next = null;
                int i = indexOf(hash(e.key), cap);
                if (tar[i] != null) {
                    e.next = tar[i];
                    count ++;
                }
                tar[i] = e;
            } while ((e = n) != null);
        }
        return count;
    }

    public int get(byte[] key) {
        if (data == null) return 0;
        int i = indexOf(hash(key), capacity);
        Entry e = data[i];
        while (e != null) {
            if (Arrays.equals(e.key, key)) {
                return e.val;
            }
            e = e.next;
        }
        return 0;
    }

    public int getOrDefault(byte[] key, int def) {
        if (data == null) return def;
        int i = indexOf(hash(key), capacity);
        Entry e = data[i];
        while (e != null) {
            if (Arrays.equals(e.key, key)) {
                return e.val;
            }
            e = e.next;
        }
        return def;
    }

    public int remove(byte[] key) {
        if (data == null) return 0;
        int i = indexOf(hash(key), capacity);
        Entry e = data[i], last = null;
        while (e != null) {
            if (Arrays.equals(e.key, key)) {
                if (last == null) {
                    data[i] = e.next;
                } else {
                    last.next = e.next;
                    linkCount--;
                }
                size--;
                return e.val;
            }
            last = e;
            e = e.next;
        }
        return 0;
    }

    public void clear() {
        if (data != null) {
            Arrays.fill(data, null);
            size = 0;
            linkCount = 0;
        }
    }

    private static int hash(byte[] array) {
        int h;
        return array == null ? 0 : (h = Arrays.hashCode(array)) & (h >>> 16);
    }

    private static int indexOf(int h, int length) {
        return h & (length - 1);
    }

    private static class Entry {
        private Entry next = null;
        private final byte[] key;
        private int val;

        private Entry(byte[] key, int val) {
            this.key = key;
            this.val = val;
        }
    }

}
