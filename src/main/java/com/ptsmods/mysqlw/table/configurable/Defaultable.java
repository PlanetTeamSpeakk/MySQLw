package com.ptsmods.mysqlw.table.configurable;

/**
 * Represents a configurable that may have a default value.
 * @param <T> The type of the value required to configure this configurable.
 * @param <S> The type of the interface extending this interface.
 */
public interface Defaultable<T, S> {

    /**
     * Configures this configurable with the set default value if one is set.
     * Throws an {@link UnsupportedOperationException} if one isn't set instead.
     * @return The configured type string.
     */
    default String applyDefault() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return Whether this defaultable has a default value set.
     */
    default boolean hasDefault() {
        return false;
    }

    /**
     * @param def The default value to use.
     * @return A version of this configurable with a default value.
     */
    S withDefault(T def);
}
