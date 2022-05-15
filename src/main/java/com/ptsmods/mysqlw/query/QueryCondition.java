package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;

/**
 * A condition a query must meet to affect or return rows.
 */
public abstract class QueryCondition {

	// All methods return an instance of QueryConditions
	// rather than QueryCondition to ease chaining.
    public static QueryConditions func(QueryFunction function) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return function.toString();
            }
        });
    }

    public static QueryConditions equals(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` = " + Database.getAsString(value);
            }
        });
    }

    public static QueryConditions notEquals(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` <> " + Database.getAsString(value);
            }
        });
    }

    public static QueryConditions greater(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` > " + Database.getAsString(value);
            }
        });
    }

    public static QueryConditions greaterEqual(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` >= " + Database.getAsString(value);
            }
        });
    }

    public static QueryConditions less(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` < " + Database.getAsString(value);
            }
        });
    }

    public static QueryConditions lessEqual(String key, Object value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` <= " + Database.getAsString(value);
            }
        });
    }

    public static QueryConditions like(String key, String value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` LIKE " + Database.enquote(value);
            }
        });
    }

    public static QueryConditions match(String key, String value) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` MATCH " + Database.enquote(value);
            }
        });
    }

    public static QueryConditions in(String key, Object[] objects) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder("`").append(key).append("` IN (");
                for (Object o : objects)
                    s.append(Database.getAsString(o)).append(", ");
                return s.delete(s.length()-2, s.length()).append(")").toString();
            }
        });
    }

    public static QueryCondition notIn(String key, Object[] objects) {
        return QueryConditions.create(new QueryCondition() {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder("`").append(key).append("` NOT IN (");
                for (Object o : objects)
                    s.append(Database.getAsString(o)).append(", ");
                return s.delete(s.length()-2, s.length()).append(")").toString();
            }
        });
    }

    @Override
    public abstract String toString();
}
