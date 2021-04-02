package com.ptsmods.mysqlw.query;

import org.jetbrains.annotations.NotNull;

/**
 * To make sure that functions don't get enquoted when putting them in a query, wrap them in this class.
 * An example would be {@code JSON_CONTAINS(`column`, '{"value": {"child": 7}}')} or {@code GeomFromText('POINT(42.8, 69.7)')} although that one is obsolete.
 * This class also implements {@link CharSequence} so that it can be passed as a key when selecting. (Otherwise it gets put between graves unless it's an asterisk.)
 */
public class QueryFunction implements CharSequence {

    private final String function;

    /**
     * Creates a new QueryFunction.
     * @param function The function this QueryFunction should contain
     */
    public QueryFunction(String function) {
        this.function = function;
    }

    /**
     * @return The function this QueryFunction contains.
     */
    public String getFunction() {
        return function;
    }

    @Override
    public int length() {
        return function.length();
    }

    @Override
    public char charAt(int index) {
        return function.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return function.subSequence(start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return function;
    }
}
