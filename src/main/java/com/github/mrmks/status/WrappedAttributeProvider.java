package com.github.mrmks.status;

import com.github.mrmks.status.api.IAttributeProvider;
import com.github.mrmks.status.api.WritingStatus;

class WrappedAttributeProvider<T> {
    final String name;
    final IAttributeProvider<T> provider;
    short[] ids;
    int index;

    WrappedAttributeProvider(String name, IAttributeProvider<T> provider) {
        this.name = name;
        this.provider = provider;
        this.ids = null;
    }

    @Deprecated
    void setIds(short[] ids) {
        this.ids = ids;
    }

    void update(T entity, WritingStatus ws) {
        provider.update(ids, entity, ws);
    }
}
