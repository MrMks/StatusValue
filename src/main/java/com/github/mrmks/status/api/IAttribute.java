package com.github.mrmks.status.api;

/**
 * The interface for attributes.
 *
 * The program do not save values for attributes. The values are provided by providers.
 */
public interface IAttribute<T> {

    /**
     * The name of this attribute, should less than 32 characters and not contains ':'
     * The full name of an attribute will be extensionName:attributeName
     */
    String getName();

    void update(T entity, int prev, int now);

}
