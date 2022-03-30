package com.github.mrmks.status.adapt;

import com.github.mrmks.status.adapt.data.StorePair;
import com.github.mrmks.status.adapt.data.ValuePair;

import java.io.IOException;
import java.util.OptionalInt;

public interface IEntityDataAccessor {

    ValuePair[] readValue() throws IOException;
    void writeValue(String[] keys, int[] vs) throws IOException;

    StorePair[] readStore() throws IOException;
    void writeStore(String[] keys, int[][] vs) throws IOException;

    @Deprecated OptionalInt getValue(int resourceId);
    @Deprecated int[] getValue();
    @Deprecated void writeValue(int resourceId, int value) throws IOException;
    @Deprecated void writeValue(int[] values) throws IOException;

    @Deprecated int[][] getStore();
    @Deprecated void writeStore(int id, int[] store) throws IOException;
    @Deprecated void writeStore(int[][] store) throws IOException;

    /**
     * After this method, any method invocation of this object should not perform anything.
     */
    void flushAndClose() throws IOException;

}
