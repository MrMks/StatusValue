package com.github.mrmks.status.adapt;

import java.io.IOException;
import java.util.OptionalInt;

public interface IEntityDataAccessor {

    OptionalInt getValue(int resourceId);
    void writeValue(int resourceId, int value) throws IOException;
    void writeValue(int[] values) throws IOException;

    int[][] getStore();
    void writeStore(int id, int[] store) throws IOException;
    void writeStore(int[][] store) throws IOException;

    /**
     * After this method, any method invocation of this object should not perform anything.
     */
    void flushAndClose() throws IOException;

}
