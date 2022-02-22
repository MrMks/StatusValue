package com.github.mrmks.status.adapt;

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
}
