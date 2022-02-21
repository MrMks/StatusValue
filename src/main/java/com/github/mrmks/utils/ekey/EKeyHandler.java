package com.github.mrmks.utils.ekey;

import java.util.Set;

/**
 * It seems that the hashCode() method and equals() method is not necessary for this class
 * as the instance of handler about type T is designed to be alone in any system
 * @param <T>
 */
public final class EKeyHandler<T extends EKey> {
    private final String[] strCache;
    private final EKey[] aryCache;

    EKeyHandler(Set<String> set, EKeyBuilder.EKeyGenerator<T> inst) {
        strCache = set.stream().sorted(this::compareString).toArray(String[]::new);
        aryCache = new EKey[set.size()];

        for (int i = 0; i < strCache.length; i++) aryCache[i] = inst.create(strCache[i], i);
    }

    public T of(String key) {
        return of(findIndex(key));
    }

    public T of(int index) {
        if (index < 0 || index >= aryCache.length) return null;
        return castT(index);
    }

    @SuppressWarnings("unchecked")
    private T castT(int i) {
        return (T) aryCache[i];
    }

    public boolean contain(String key) {
        return !(findIndex(key) < 0);
    }

    private int findIndex(String k) {
        if (k == null || strCache.length == 0) return -1;

        int bi = 0, ei = aryCache.length - 1, i, r;

        if ((r = compareString(strCache[bi], k)) >= 0) return r == 0 ? bi : -1;
        if (bi == ei) return -1;
        if ((r = compareString(strCache[ei], k)) <= 0) return r == 0 ? ei : -1;

        while (bi < ei && ei - bi > 1) {
            String s = strCache[i = bi + (ei - bi) / 2];
            r = compareString(s, k);
            if (r == 0) return i;
            else if (r < 0) bi = i;
            else ei = i;
        }

        return -1;
    }

    private int compareString(String s1, String s2) {
        boolean t1 = s1 == null, t2 = s2 == null;
        return t1 || t2 ? (t1 && t2 ? 0 : (t1 ? -1 : 1)) : compareStringNoNull(s1, s2);
    }

    private int compareStringNoNull(String s1, String s2) {
        return s1.length() != s2.length() ? (s1.length() - s2.length()) : (s1.isEmpty() ? 0 : cmpStr(s1, s2));
    }

    private int cmpStr(String s1, String s2) {
        int r = 0, i = -1;
        while (r == 0 && (++i) < s1.length()) r = s1.charAt(i) - s2.charAt(i);
        return r;
    }

    public int size() {
        return aryCache.length;
    }
}
