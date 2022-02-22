package com.github.mrmks.status;

import com.github.mrmks.status.api.IAttribute;
import com.github.mrmks.status.api.IResource;

abstract class WrappedAttribute<T> {

    final IAttribute<T> attribute;
    final String name;
    final byte flag;
    private final boolean special, step;

    WrappedAttribute(IAttribute<T> attr, String name, byte flag) {
        this.attribute = attr;
        this.name = name;
        this.flag = (byte) (flag & 0x1f);
        this.special = flag > 1;
        this.step = flag > 2;
    }

    void update(T entity, int prev, int now) {
        if (special) {
            IResource<T> ir = (IResource<T>) attribute;
            if (step) ir.updateStep(entity, prev, now);
            else ir.updateUBound(entity, prev, now);
        } else {
            attribute.update(entity, prev, now);
        }
        /*
        if (attribute instanceof IResource && (flag & (FLAG_SPECIAL | FLAG_STEP)) > 0) {
            IResource<T> ir = (IResource<T>) attribute;
            if ((flag & FLAG_STEP) > 0) ir.updateUBound(entity, prev, now);
            else ir.updateStep(entity, prev, now);
        } else {
            attribute.update(entity, prev, now);
        }
         */
    }

}
