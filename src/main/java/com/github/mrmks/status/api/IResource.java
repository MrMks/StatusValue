package com.github.mrmks.status.api;

import java.util.OptionalInt;

/**
 * Attributes interface with auto growth.
 *
 * The program should store the values.
 * @param <T>
 */
public interface IResource<T> extends IAttribute<T> {

    /**
     * The name should shorter than 30 characters.
     */
    @Override
    String getName();

    /*
     * We store values, upbounds and steps in StatusTable with base added, but we store value without base via IDataAccessor.
     * They should be constant numbers, we may cache those in some build.
     */

    int baseValue();
    int baseUBound();
    int baseStep();

    /**
     * The interval should be a const number, we may cache it in some build.
     */
    int interval();

    /**
     * Can we auto update with step when zhe number is zero ?
     */
    default boolean canStepZero() {
        return false;
    }

    void updateUBound(T entity, int prev, int now);
    void updateStep(T entity, int prev, int now);

    default int valueVersion() {
        return 0;
    }

    default Updater valueUpdater() {
        return null;
    }

    interface Updater {
        OptionalInt accept(int srcVer, int tarVer, int prevData);
    }
}
