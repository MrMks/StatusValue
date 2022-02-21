package com.github.mrmks.utils.ekey;

import java.util.Collection;
import java.util.HashSet;

public final class EKeyBuilder<T extends EKey> {

    private HashSet<String> set = new HashSet<>();
    private boolean built = false;
    /**
     * Add strings to this builder
     * @param str should not be null, or an NPE will be thrown
     */
    public EKeyBuilder<T> add(String... str) {
        for (String s : str) add0(s);
        return this;
    }

    public EKeyBuilder<T> add(Collection<String> str) {
        for (String s : str) add0(s);
        return this;
    }

    private void add0(String str) {
        if (built) throw new EKeyBuilderFinishedException();
        if (str == null) throw new NullPointerException();
        set.add(str);
    }

    public EKeyHandler<T> build(EKeyGenerator<T> kGen) {
        if (built) throw new EKeyBuilderFinishedException();
        EKeyHandler<T> handler = new EKeyHandler<>(set, kGen);
        set.clear();
        set = null;
        built = true;
        return handler;
    }

    /**
     * Used to check if this builder is still active
     * @return true if the build method was once called
     */
    public boolean isBuilt() {
        return built;
    }

    public static class EKeyBuilderFinishedException extends Error {}

    public interface EKeyGenerator<K extends EKey> {
        K create(String n, int o);
    }
}
