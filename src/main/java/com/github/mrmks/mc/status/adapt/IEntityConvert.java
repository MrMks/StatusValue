package com.github.mrmks.mc.status.adapt;

public interface IEntityConvert<T> {
    T fromBytes(byte[] bytes);
    byte[] toBytes(T token);
}
