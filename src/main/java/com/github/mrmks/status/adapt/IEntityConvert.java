package com.github.mrmks.status.adapt;

import java.util.Arrays;

public interface IEntityConvert<T> {
    /*
     * The bytes are always be non-null, but you can return null here when you can't find the entity.
     */
    T fromBytes(byte[] bytes);

    /*
     * You can't return null in any condition besides the obj is null;
     * Also you can't return empty byte array in any condition since it is used by system;
     */
    byte[] toBytes(T obj);

    /*
     * This method allow you to provide a way to convert your hard debugging byte array to a familiar readable string
     */
    default String familiarName(byte[] bytes) {
        return Arrays.toString(bytes);
    }
}
