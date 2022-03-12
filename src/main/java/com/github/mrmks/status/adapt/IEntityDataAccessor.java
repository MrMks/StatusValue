package com.github.mrmks.status.adapt;

import com.github.mrmks.utils.StringIntMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.OptionalInt;

public interface IEntityDataAccessor {

    StringIntMap readValue() throws IOException;
    void writeValue(String[] keys, int[] vs) throws IOException;

    HashMap<String, int[]> readStore() throws IOException;
    void writeStore(String[] keys, int[][] vs) throws IOException;

    OptionalInt getValue(int resourceId);
    int[] getValue();
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
