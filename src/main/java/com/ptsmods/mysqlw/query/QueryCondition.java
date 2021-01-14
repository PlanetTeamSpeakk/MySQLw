package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;

/**
 * A condition a query must meet to affect or return rows.
 */
public abstract class QueryCondition {

    public static QueryCondition func(QueryFunction function) {
        return new QueryCondition() {
            @Override
            public String toString() {
                return function.toString();
            }
        };
    }

    public static QueryCondition equals(String key, Object value) {
        return new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` = " + Database.getAsString(value);
            }
        };
    }

    public static QueryCondition notEquals(String key, Object value) {
        return new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` <> " + Database.getAsString(value);
            }
        };
    }

    public static QueryCondition greater(String key, Object value) {
        return new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` > " + Database.getAsString(value);
            }
        };
    }

    public static QueryCondition less(String key, Object value) {
        return new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` < " + Database.getAsString(value);
            }
        };
    }

    public static QueryCondition like(String key, String value) {
        return new QueryCondition() {
            @Override
            public String toString() {
                return "`" + key + "` LIKE " + Database.enquote(value);
            }
        };
    }

    public static QueryCondition in(String key, Object[] objects) {
        return new QueryCondition() {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder("`").append(key).append("` IN (");
                for (Object o : objects)
                    s.append(Database.getAsString(o)).append(", ");
                return s.delete(s.length()-2, s.length()).append(")").toString();
            }
        };
    }

    public static QueryCondition notIn(String key, Object[] objects) {
        return new QueryCondition() {
            @Override
            public String toString() {
                StringBuilder s = new StringBuilder("`").append(key).append("` NOT IN (");
                for (Object o : objects)
                    s.append(Database.getAsString(o)).append(", ");
                return s.delete(s.length()-2, s.length()).append(")").toString();
            }
        };
    }

    QueryCondition() {}

    @Override
    public abstract String toString();

}
