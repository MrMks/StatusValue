package com.github.mrmks.status.api.simple;

public interface LambdaUpdate<T> {
    void update(T entity, int prev, int now);
}
