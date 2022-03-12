package com.github.mrmks.status.adapt;

import com.github.mrmks.status.api.IModifier;
import com.github.mrmks.status.api.IResource;

import java.io.IOException;
import java.util.OptionalInt;

/**
 * there are two things to read or write.
 * one is the value stored for IResources
 * another is the int array stored for IAttributes
 * we store the values with two key: entity save key{@link IEntityConvert#toBytes(Object)} and attribute name;
 * and we also store the version of the data we stored, so we can update it with {@link IModifier.Updater} and {@link IResource.Updater};
 */
public interface IDataAccessor {
    /**
     * Connect to the database and validate the connection.
     */
    void connect() throws IOException;

    /**
     * update methods only called once for each attribute in one life-cycle;
     */
    @Deprecated
    void updateValue(String[] resourceName, int[] valueVersion, IResource.Updater[] updater);
    @Deprecated
    void updateStore(String[] modifierName, int[] storeVersion, byte[] storeSize, IModifier.Updater[] updater);
    @Deprecated
    void updateFinish();

    IEntityDataAccessor withEntity(byte[] entityKey) throws IOException;

    /**
     * Your implementation can buffer some data for better performance, but when we call flush, your implementation should write them to disk immediately.
     */
    void flushAll() throws IOException;
    void close();

}