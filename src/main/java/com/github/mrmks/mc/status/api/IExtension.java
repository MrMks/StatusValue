package com.github.mrmks.mc.status.api;

import java.util.Collections;
import java.util.List;

public interface IExtension<T> {

    String getPrefix();

    /*
     * Both attribute and resource are registered here.
     */
    default List<IAttribute<T>> getAttributes() {
        return Collections.emptyList();
    }

    default List<IAttributeProvider<T>> getAttributeProviders() {
        return Collections.emptyList();
    }

    /*
     * Register modifiers.
     */
    default List<IModifier> getModifiers() {
        return Collections.emptyList();
    }

    default List<IHandler> getHandlers() {
        return Collections.emptyList();
    }

}
