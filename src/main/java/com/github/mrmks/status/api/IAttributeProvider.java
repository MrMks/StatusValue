package com.github.mrmks.status.api;

public interface IAttributeProvider<T> {

    String getName();

    SimpleDependency[] getDependencies();

    void update(short[] ids, T entity, WritingStatus ws);
}
