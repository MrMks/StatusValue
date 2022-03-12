package com.github.mrmks.utils;

import java.util.Arrays;

public class StringIntMap {
    private int cap;
    private int size = 0;
    private int linkCount = 0;
    private String[][] ks;
    private int[][] vs;

    public StringIntMap() {
        this.cap = 16;
    }

    public StringIntMap(int initCapacity) {
        this.cap = 16;
        initCapacity = Math.min(initCapacity, 0x40000000);
        while (this.cap < (initCapacity >>> 4)) this.cap <<= 4;
        while (this.cap < initCapacity) this.cap <<= 1;
    }

    private static int hash(String key) {
        int h;
        return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private static int indexOf(int h, int length) {
        return h & (length - 1);
    }

    private static boolean keyEqual(String k1, String k2) {
        return k1 == null && k2 == null || k1 != null && k1.equals(k2);
    }

    public int put(String key, int val) {
        if (vs == null) {
            ks = new String[cap][];
            vs = new int[cap][];
        }
        int i = indexOf(hash(key), cap);
        int[] v = vs[i];
        if (v == null) {
            v = vs[i] = new int[5];
            (ks[i] = new String[4])[0] = key;
            v[0] = 1;
            v[1] = val;
            size++;
        } else if (v[0] == 0) {
            v[0] = 1;
            v[1] = val;
            ks[i][0] = key;
            size++;
        } else {
            String[] k = ks[i];
            for (int j = 1; j < v[0] + 1; j++) {
                if (keyEqual(k[j - 1], key)) {
                    int r = v[j];
                    v[j] = val;
                    return r;
                }
            }
            linkCount++;
            size++;
            if (v[0] + 1 >= v.length) {
                vs[i] = v = Arrays.copyOf(v, (v.length << 1) - 1);
                ks[i] = k = Arrays.copyOf(k, k.length << 1);
            }
            v[0] ++;
            v[v[0]] = val;
            k[v[0] - 1] = key;

            if (size >= cap && linkCount > (cap >>> 2) && (cap > 0 && cap <= 0x40000000)) {
                int nextCap = cap << 1;
                String[][] nextKs = new String[nextCap][];
                int[][] nextVs = new int[nextCap][];

                linkCount = 0;
                for (i = 0; i < cap; i++) {
                    if (vs[i] != null && vs[i][0] > 0) {
                        v = nextVs[i] = vs[i];
                        k = nextKs[i] = ks[i];
                        int[] nv = new int[v.length];
                        String[] nk = new String[k.length];
                        int n0 = 1, n1 = 1;
                        for (int j = 1; j < v[0] + 1; j++) {
                            int ni = indexOf(hash(k[j - 1]), nextCap);
                            if (ni != i) {
                                nv[n1] = v[j];
                                nk[n1 - 1] = k[j - 1];
                                n1++;
                                k[j - 1] = null;
                            } else {
                                if (n0 < j) {
                                    v[n0] = v[j];
                                    k[n0 - 1] = k[j - 1];
                                    k[j - 1] = null;
                                }
                                n0 ++;
                            }
                        }
                        v[0] = n0 - 1;
                        nv[0] = n1 - 1;
                        if (n0 > 2) linkCount += n0 - 2;
                        if (n1 >= 2) {
                            linkCount += n1 - 2;
                            nextKs[i + cap] = nk;
                            nextVs[i + cap] = nv;
                        }
                    }
                }
                cap = nextCap;
                ks = nextKs;
                vs = nextVs;
            }
        }
        return 0;
    }

    public int get(String key) {
        return getOrDefault(key, 0);
    }

    public int getOrDefault(String key, int def) {
        if (vs != null) {
            int h = hash(key);
            int i = indexOf(h, cap);
            int[] v = vs[i];
            if (v != null) {
                String[] k = ks[i];
                for (int j = 1; j < v[0] + 1; j++)
                    if (keyEqual(k[j - 1], key))
                        return v[j];
            }
        }
        return def;
    }

    public int remove(String key) {
        if (vs != null) {
            int h = hash(key);
            int i = indexOf(h, cap);
            int[] v = vs[i];
            if (v != null) {
                String[] k = ks[i];
                for (int j = 1; j < v[0] + 1; j++) {
                    if (keyEqual(k[j - 1], key)) {
                        int r = v[j];
                        if (j < v[0]) {
                            System.arraycopy(k, j, k, j - 1, v[0] - j);
                            System.arraycopy(v, j + 1, v, j, v[0] - j);
                        }
                        k[v[0] - 1] = null;
                        size --;
                        if (v[0] > 1) linkCount--;
                        v[0] --;
                        return r;
                    }
                }
            }
        }
        return 0;
    }

    public boolean containKey(String key) {
        if (vs != null) {
            int h = hash(key);
            int i = indexOf(h, cap);
            int[] v = vs[i];
            if (v != null) {
                String[] k = ks[i];
                for (int j = 1; j < v[0]; j++)
                    if (keyEqual(k[j - 1], key)) return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (String[] k : ks) if (k != null) Arrays.fill(k, null);
        for (int[] v : vs) if (v != null) v[0] = 0;
        linkCount = 0;
        size = 0;
    }
}
