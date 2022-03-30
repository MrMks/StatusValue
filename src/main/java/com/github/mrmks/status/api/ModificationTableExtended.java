package com.github.mrmks.status.api;

public interface ModificationTableExtended extends ModificationTable {

    @Deprecated
    void buffAttribute(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int duration, boolean anySource, boolean canRemove, boolean reverse);
    @Deprecated
    void buffResource(String key, String tag, String icon, BuffType type, int[] idTar, int[] valTar, int[] idSrc, int[] valSrc, int interval, int count, boolean anySource, boolean canRemove, boolean reverse);
    @Deprecated
    void buffModifier(String key, String tag, String icon, BuffType type, int id, int[] val, int interval, int count, boolean anySource, boolean canRemove, boolean reverse);

    @Deprecated
    void removeBuffSrc(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force);
    @Deprecated
    void removeBuffTar(BuffType type, String key, String tag, boolean anySource, boolean once, boolean force);


}
