package com.github.mrmks.mc.status.api;

import com.github.mrmks.mc.status.utils.SimpleDependency;

public interface IAttributeProvider<T> {

    String getName();

    SimpleDependency[] getDependencies();

    void update(short[] ids, T entity, WritingStatus ws);
}
