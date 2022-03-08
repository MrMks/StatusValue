package com.github.mrmks.status.api.simple;

import com.github.mrmks.status.api.IResource;

public class SimpleResource<T> extends SimpleAttribute<T> implements IResource<T> {

    private final int base, upbound, step, interval;
    private final LambdaUpdate<T> lambdaU, lambdaS;
    private final boolean updateWhenZero;
    public SimpleResource(String name, int base, int upbound, int step, int interval, boolean updateWhenZero, LambdaUpdate<T> lambdaN, LambdaUpdate<T> lambdaU, LambdaUpdate<T> lambdaS) {
        super(name, lambdaN);
        this.base = base;
        this.upbound = upbound;
        this.step = step;
        this.interval = interval;
        this.lambdaU = lambdaU;
        this.lambdaS = lambdaS;
        this.updateWhenZero = updateWhenZero;
    }

    public SimpleResource(String name, int base, int upbound, int step, int interval, boolean updateWhenZero, LambdaUpdate<T> lambdaN) {
        super(name, lambdaN);
        this.base = base;
        this.upbound = upbound;
        this.step = step;
        this.interval = interval;
        this.lambdaU = null;
        this.lambdaS = null;
        this.updateWhenZero = updateWhenZero;
    }

    public SimpleResource(String name, int base, int upbound, int step, int interval, boolean updateWhenZero) {
        super(name, null);
        this.base = base;
        this.upbound = upbound;
        this.step = step;
        this.interval = interval;
        this.lambdaU = null;
        this.lambdaS = null;
        this.updateWhenZero = updateWhenZero;
    }

    public SimpleResource(String name, int base, int upbound, int step, int interval) {
        super(name, null);
        this.base = base;
        this.upbound = upbound;
        this.step = step;
        this.interval = interval;
        this.lambdaU = null;
        this.lambdaS = null;
        this.updateWhenZero = IResource.super.canStepZero();
    }

    @Override
    public int baseValue() {
        return base;
    }

    @Override
    public int baseUBound() {
        return upbound;
    }

    @Override
    public int baseStep() {
        return step;
    }

    @Override
    public int interval() {
        return interval;
    }

    @Override
    public boolean canStepZero() {
        return updateWhenZero;
    }

    @Override
    public void updateUBound(T entity, int prev, int now) {
        if (lambdaU != null) lambdaU.update(entity, prev, now);
    }

    @Override
    public void updateStep(T entity, int prev, int now) {
        if (lambdaS != null) lambdaS.update(entity, prev, now);
    }
}
