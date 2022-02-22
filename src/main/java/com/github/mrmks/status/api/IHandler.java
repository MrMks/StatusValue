package com.github.mrmks.status.api;

public interface IHandler {
    /**
     * The name of this handler, length bigger than 0 and shorter than 32
     */
    String getName();

    /**
     * The modifierName with prefix this handler going to handle.
     */
    SimpleDependency[] targetModifiers();

    /**
     * The attribute value this handler going to read.
     * We will return the ids in same order in {@link #handle(int, ModificationEvent, short[], int[])};
     */
    SimpleDependency[] targetAttributes();

    /**
     * Any handler can require a space to read params.
     * If your handler require some params to run, change the number of this method return.
     * For performance reasons, you can define the size no larger than 16.
     */
    default int paramSize() {
        return 0;
    }

    /**
     * This defines the order of the handlers.
     * You can define before or after some handlers and defines is those handlers are required.
     * If required handler doesn't exist, this handler will be removed.
     */
    SortableDependency[][] getDependencies();

    /**
     * This defines should or not we call {@link #handle(int, ModificationEvent, short[], int[])} when the event is canceled.
     */
    default boolean handleCanceled() {
        return false;
    }

    /**
     * Handle the modificationEvent. Only do modifications on the event is suggested.
     * @param modifierIndex modifier id.
     * @param event the event.
     * @param ids The ids of the attribute requires in {@link #targetAttributes()}, in same order. minus means the attribute doesn't exist.
     * @param params the params the user passed. the size may smaller than paramSize;
     */
    void handle(int modifierIndex, ModificationEvent event, short[] ids, int[] params);
}
