package com.github.mrmks.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;

public class CommonUtils {

    public static <T> void sortBy(T[] ary, Comparator<T> cmp, Object[]... os) {
        int[][] sorter = new int[ary.length][];
        for (int i = 0; i < ary.length; i++) sorter[i] = new int[]{i};
        Arrays.sort(sorter, (a,b) -> cmp.compare(ary[a[0]], ary[b[0]]));
        for (int i = 0; i < ary.length; i++) {
            if (sorter[i][0] != i) {
                T tmp = ary[i];
                Object[] tmpOs = new Object[os.length];
                for (int k = 0; k < os.length; k++) tmpOs[k] = os[k][i];
                int j = i;
                do {
                    int t = sorter[j][0];
                    sorter[j][0] = j;
                    ary[j] = ary[t];
                    for (int k = 0; k < os.length; k++) os[k][j] = os[k][t];
                    j = t;
                } while (sorter[j][0] != i);
                ary[j] = tmp;
                for (int k = 0; k < os.length; k++) os[k][j] = tmpOs[k];
                sorter[j][0] = j;
            }
        }
    }

    public static <T extends Comparable<T>> void sortBy(T[] ary, Object[]... os) {
        if (os.length == 0) {
            Arrays.sort(ary);
        } else {
            int[][] sorter = new int[ary.length][];
            for (int i = 0; i < ary.length; i++) sorter[i] = new int[]{i};
            Arrays.sort(sorter, Comparator.comparing(i -> ary[i[0]]));
            for (int i = 0; i < ary.length; i++) {
                if (sorter[i][0] != i) {
                    T tmp = ary[i];
                    Object[] tmpOs = new Object[os.length];
                    for (int k = 0; k < os.length; k++) tmpOs[k] = os[k][i];
                    int j = i;
                    do {
                        int t = sorter[j][0];
                        sorter[j][0] = j;
                        ary[j] = ary[t];
                        for (int k = 0; k < os.length; k++) os[k][j] = os[k][t];
                        j = t;
                    } while (sorter[j][0] != i);
                    ary[j] = tmp;
                    for (int k = 0; k < os.length; k++) os[k][j] = tmpOs[k];
                    sorter[j][0] = j;
                }
            }
        }
    }

    public static void sortBy(int[] ary, Object[]... os) {
        int[][] sorter = new int[ary.length][];
        for (int i = 0; i < ary.length; i++) sorter[i] = new int[]{i};
        Arrays.sort(sorter, Comparator.comparingInt(i -> ary[i[0]]));
        for (int i = 0; i < ary.length; i++) {
            if (sorter[i][0] != i) {
                int tmp = ary[i];
                Object[] tmpOs = new Object[os.length];
                for (int k = 0; k < os.length; k++) tmpOs[k] = os[k][i];
                int j = i;
                do {
                    int t = sorter[j][0];
                    sorter[j][0] = j;
                    ary[j] = ary[t];
                    for (int k = 0; k < os.length; k++) os[k][j] = os[k][t];
                    j = t;
                } while (sorter[j][0] != i);
                ary[j] = tmp;
                for (int k = 0; k < os.length; k++) os[k][j] = tmpOs[k];
                sorter[j][0] = j;
            }
        }
    }

    public static <T, U> int binarySearch(T[] ary, Function<? super T, ? extends U> toKey, U key, Comparator<? super U> cmp) {
        int left = 0, right = ary.length - 1;

        while (left <= right) {
            int mid = (right - left) >>> 1;
            U midVal = toKey.apply(ary[mid]);
            int cmpR = cmp.compare(key, midVal);
            if (cmpR < 0) {
                right = mid - 1;
            } else if (cmpR > 0) {
                left = mid + 1;
            } else return mid;
        }
        return -(left + 1);
    }

}
