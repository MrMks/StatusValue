package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.api.IAttribute;
import com.github.mrmks.mc.status.api.IResource;

import static com.github.mrmks.mc.status.Constants.*;

abstract class WrappedAttribute<T> {

    final IAttribute<T> attribute;
    final String name;
    final byte flag;

    WrappedAttribute(IAttribute<T> attr, String name, byte flag) {
        this.attribute = attr;
        this.name = name;
        this.flag = (byte) (flag & 0x1f);
    }

    void update(T entity, int prev, int modified) {
        if (attribute instanceof IResource && (flag & (FLAG_SPECIAL | FLAG_STEP)) > 0) {
            IResource<T> ir = (IResource<T>) attribute;
            if ((flag & FLAG_STEP) > 0) ir.updateUBound(entity, prev, modified);
            else ir.updateStep(entity, prev, modified);
        } else {
            attribute.update(entity, prev, modified);
        }
    }

    abstract short getId();
    abstract short getDataId();
    abstract WrappedAttribute<T> copy();
}
