package com.github.mrmks.mc.status.adapt;

import com.github.mrmks.mc.status.api.IModifier;
import com.github.mrmks.mc.status.api.IResource;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * there are two things to read or write.
 * one is the value stored for IResources
 * another is the int array stored for IAttributes
 * we store the values with two key: entity save key{@link IEntityConvert#toBytes(Object)} )} and attribute name;
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
    void updateValue(String[] resourceName, int[] valueVersion, IResource.Updater[] updater);
    void updateStore(String[] modifierName, int[] storeVersion, byte[] storeSize, IModifier.Updater[] updater);

    void updateFinish();

    OptionalInt getValue(byte[] entityKey, int resourceId);
    void writeValue(byte[] entityKey, int[] values);
    void writeValue(byte[] entityKey, int resourceId, int value);

    int[][] getStore(byte[] entityKey);
    void writeStore(byte[] entityKey, int[][] store);
    void writeStore(byte[] entityKey, int id, int[] val);

    void writeBuff(byte[] entityKey, byte[][] data);
    byte[][] readBuff(byte[] entityKey);

    IEntityDataAccessor withEntity(byte[] entityKey);

    /**
     * Your implementation can buffer some data for better performance, but when we call flush, your implementation should write them to disk immediately.
     */
    void flushEntity(byte[] entityKey);
    void flushAll();
    void close();

}