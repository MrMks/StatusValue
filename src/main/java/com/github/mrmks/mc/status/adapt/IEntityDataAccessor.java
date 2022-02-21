package com.github.mrmks.mc.status.adapt;

import java.util.OptionalInt;

public interface IEntityDataAccessor {

    OptionalInt getValue(int resourceId);
    void writeValue(int resourceId, int value);
    void writeValue(int[] values);

    int[][] getStore();
    void writeStore(int id, int[] store);
    void writeStore(int[][] store);

    void writeBuff(byte[][] data);
    byte[][] readBuff();

    /**
     * After this method, any method invocation of this object should not perform anything.
     */
    void flushAndClose();

}