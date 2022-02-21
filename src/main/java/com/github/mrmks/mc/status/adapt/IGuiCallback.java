package com.github.mrmks.mc.status.adapt;

public interface IGuiCallback {

    void updateAttribute(byte[] tar, String name, int value);

    void updateResource(byte[] tar, String name, int value);

    /**
     * @param key The key must be non-null, which will be used to match buffs.
     * @param src Only if src matched or the src is empty or null can show the two buffs may equal.
     */
    void updateBuff(byte[] tar, String key, byte[] src, boolean anySource, String icon, int duration);
    void removeBuff(byte[] tar, String key, byte[] src, boolean anySource);
}
