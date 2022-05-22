package com.ptsmods.mysqlw.procedure;

import com.ptsmods.mysqlw.Database;

import java.util.function.Function;

public class ConditionValue {
    private final Type type;
    private final Object value;

    private ConditionValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static ConditionValue sqlError(int error) {
        return new ConditionValue(Type.SQL_ERROR, error);
    }

    public static ConditionValue sqlState(String state) {
        return new ConditionValue(Type.SQL_STATE, state);
    }

    public static ConditionValue condition(String conditionName) {
        return new ConditionValue(Type.CONDITION, conditionName);
    }

    public static ConditionValue sqlWarning() {
        return new ConditionValue(Type.SQL_WARNING, null);
    }

    public static ConditionValue notFound() {
        return new ConditionValue(Type.NOT_FOUND, null);
    }

    public static ConditionValue sqlException() {
        return new ConditionValue(Type.SQL_EXCEPTION, null);
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString(value);
    }

    public enum Type {
        SQL_ERROR(v -> String.valueOf(v)),
        SQL_STATE(v -> "SQLSTATE " + Database.enquote((String) v)),
        CONDITION(v -> (String) v),
        SQL_WARNING(v -> "SQLWARNING"),
        NOT_FOUND(v -> "NOT FOUND"),
        SQL_EXCEPTION(v -> "SQLEXCEPTION");

        private final Function<Object, String> formatter;

        Type(Function<Object, String> formatter) {
            this.formatter = formatter;
        }

        public String toString(Object value) {
            return formatter.apply(value);
        }
    }
}
