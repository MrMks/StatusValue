package com.github.mrmks.status;

import com.github.mrmks.status.api.IModifier;
import com.github.mrmks.status.api.ModificationCache;

abstract class WrappedModifier {
    final IModifier modifier;
    final String name;
    short dataIndex = -1;

    WrappedModifier(IModifier modifier, String name) {
        this.modifier = modifier;
        this.name = name;
    }

    protected abstract short index();

    protected abstract short[] getIds();
    protected abstract short[] getSortedIds();
    protected abstract int handlerSize();

    void handle(int[] value, ModificationCache table, int session, int[] src, int[] tar) {
        modifier.handle(getIds(), value, table, session, src, tar);
    }

}
