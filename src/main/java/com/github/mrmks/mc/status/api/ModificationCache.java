package com.github.mrmks.mc.status.api;

public interface ModificationCache {
    int getSrc(int id);
    int getTar(int id);

    void modifySrc(int id, int val);
    void modifyTar(int id, int val);

    void buffAttribute(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int duration, boolean anySource, boolean canRemove, boolean reverse);
    void buffResource(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int interval, int count, boolean anySource, boolean canRemove, boolean reverse);
    void buffResource(String key, String tag, String icon, BuffType type, int id, int[] val, int interval, int count, boolean anySource, boolean canRemove, boolean reverse);

    void removeBuffSrc(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force);
    void removeBuffTar(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force);
}
