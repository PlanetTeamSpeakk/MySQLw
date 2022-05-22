package com.ptsmods.mysqlw.query;

import static com.ptsmods.mysqlw.Database.enquote;

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
     * Creates a basic new QueryFunction that adds two columns or variables together.
     * @param arg1 The first column or variable
     * @param arg2 The second column or variable
     * @return Addition QueryFunction
     */
    public static QueryFunction add(String arg1, String arg2) {
        return new QueryFunction(enquote(arg1) + " + " + enquote(arg2));
    }

    /**
     * Creates a basic new QueryFunction that subtracts two columns or variables from each other.
     * @param arg1 The first column or variable
     * @param arg2 The second column or variable
     * @return Subtraction QueryFunction
     */
    public static QueryFunction subtract(String arg1, String arg2) {
        return new QueryFunction(enquote(arg1) + " - " + enquote(arg2));
    }

    /**
     * Creates a basic new QueryFunction that multiplies two columns or variables.
     * @param arg1 The first column or variable
     * @param arg2 The second column or variable
     * @return Multiplication QueryFunction
     */
    public static QueryFunction multiply(String arg1, String arg2) {
        return new QueryFunction(enquote(arg1) + " * " + enquote(arg2));
    }

    /**
     * Creates a basic new QueryFunction that divides two columns or variables.
     * @param arg1 The first column or variable
     * @param arg2 The second column or variable
     * @return Division QueryFunction
     */
    public static QueryFunction divide(String arg1, String arg2) {
        return new QueryFunction(enquote(arg1) + " / " + enquote(arg2));
    }

    /**
     * Creates a basic new QueryFunction that counts rows.
     * @param arg What to count
     * @return Count QueryFunction
     */
    public static QueryFunction count(String arg) {
        return new QueryFunction("count(" + arg + ")");
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

    @Override
    public String toString() {
        return function;
    }
}
