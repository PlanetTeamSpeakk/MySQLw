package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;

/**
 * A condition a query must meet to affect or return rows.
 */
public abstract class QueryCondition {

    // All methods return an instance of QueryConditions
    // rather than QueryCondition to ease chaining.

    /**
     * @param function The function to check
     * @return A QueryCondition that checks the given function
     */
    public static QueryConditions func(QueryFunction function) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return function.toString();
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition comparing a column against a value
     */
    public static QueryConditions equals(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " = " + Database.getAsString(value);
            }
        });
    }

    /**
     * @param var1 The first variable
     * @param var2 The second variable
     * @return A QueryCondition comparing two variables
     */
    public static QueryConditions varEquals(String var1, String var2) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return var1 + " = " + var2;
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column does not equal a value
     */
    public static QueryConditions notEquals(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " <> " + Database.getAsString(value);
            }
        });
    }

    /**
     * @param var1 The first variable
     * @param var2 The second variable
     * @return A QueryCondition that checks if two variables are not equal
     */
    public static QueryConditions varNotEquals(String var1, String var2) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return var1 + " <> " + var2;
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column is greater than the given value
     */
    public static QueryConditions greater(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " > " + Database.getAsString(value);
            }
        });
    }

    /**
     * @param var1 The first variable
     * @param var2 The second variable
     * @return A QueryCondition that checks if var1 is greater than var2
     */
    public static QueryConditions varGreater(String var1, String var2) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return var1 + " > " + var2;
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column is greater than or equal to the given value
     */
    public static QueryConditions greaterEqual(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " >= " + Database.getAsString(value);
            }
        });
    }

    /**
     * @param var1 The first variable
     * @param var2 The second variable
     * @return A QueryCondition that checks if var1 is greater than or equal to var2
     */
    public static QueryConditions varGreaterEqual(String var1, String var2) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return var1 + " >= " + var2;
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column is less than the given value
     */
    public static QueryConditions less(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " < " + Database.getAsString(value);
            }
        });
    }

    /**
     * @param var1 The first variable
     * @param var2 The second variable
     * @return A QueryCondition that checks if var1 is less than var2
     */
    public static QueryConditions varLess(String var1, String var2) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return var1 + " < " + var2;
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column is less than or equal to the given value
     */
    public static QueryConditions lessEqual(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " <= " + Database.getAsString(value);
            }
        });
    }

    /**
     * @param var1 The first variable
     * @param var2 The second variable
     * @return A QueryCondition that checks if var1 is less than or equal to var2
     */
    public static QueryConditions varLessEqual(String var1, String var2) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return var1 + " <= " + var2;
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column is like the given value
     */
    public static QueryConditions like(String key, String value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " LIKE " + Database.enquote(value);
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param value The value to compare to
     * @return A QueryCondition that checks if a column matches the given value
     */
    public static QueryConditions match(String key, String value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(key) + " MATCH " + Database.enquote(value);
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param objects The objects to check
     * @return A QueryCondition that checks if a column is in the given list
     */
    public static QueryConditions in(String key, Object[] objects) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder().append(Database.engrave(key)).append(" IN (");
                for (Object o : objects)
                    s.append(Database.getAsString(o)).append(", ");
                return s.delete(s.length()-2, s.length()).append(")").toString();
            }
        });
    }

    /**
     * @param key The name of the column to compare
     * @param objects The objects to check
     * @return A QueryCondition that checks if a column is not in the given list
     */
    public static QueryConditions notIn(String key, Object[] objects) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder().append(Database.engrave(key)).append(" NOT IN (");
                for (Object o : objects)
                    s.append(Database.getAsString(o)).append(", ");
                return s.delete(s.length()-2, s.length()).append(")").toString();
            }
        });
    }

    /**
     * @param arg The name of the argument to check
     * @return A QueryCondition that checks the value of a column or variable
     */
    public static QueryConditions bool(String arg) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return Database.engrave(arg);
            }
        });
    }

    @Override
    public abstract String toString();
}
