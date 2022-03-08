package com.github.mrmks.status.api.simple;

import com.github.mrmks.status.api.IAttribute;

public class SimpleAttribute<T> implements IAttribute<T> {

    private final String name;
    private final LambdaUpdate<? super T> lambda;
    public SimpleAttribute(String name, LambdaUpdate<? super T> lambda) {
        this.name = name;
        this.lambda = lambda;
    }

    public SimpleAttribute(String name) {
        this(name, null);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void update(T entity, int prev, int now) {
        if (lambda != null) lambda.update(entity, prev, now);
    }
}
