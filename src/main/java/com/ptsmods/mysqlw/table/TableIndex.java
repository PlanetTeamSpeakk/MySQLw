package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;

/**
 * Table indices, used to speed up queries.
 */
public class TableIndex {

    public static TableIndex index(String name, String column, Type type) {
        return new TableIndex(name, column, type);
    }

    public static TableIndex index(String column, Type type) {
        return new TableIndex(null, column, type);
    }

    private final String name, column;
    private final Type type;

    private TableIndex(String name, String column, Type type) {
        this.name = name;
        this.column = column;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getColumn() {
        return column;
    }

    public Type getType() {
        return type;
    }

    @Override
    public TableIndex clone() {
        return new TableIndex(name, column, type);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeColumn) {
        return type.toString(name, column, includeColumn);
    }

    public enum Type {
        /**
         * Each row must have a unique value for this column.
         */
        UNIQUE,
        /**
         * Used to speed up queries.
         */
        INDEX,
        /**
         * Allows you to match pieces of text against the value of this column.
         * To match case-sensitively, have a look at {@link ColumnAttributes#BINARY BINARY}.
         */
        FULLTEXT,
        /**
         * Indices for {@link ColumnType#GEOMETRY GEOMETRY} objects.
         */
        SPATIAL;

        public String toString(String name, String column, boolean includeColumn) {
            return (this == INDEX ? "" : name() + " ") + "INDEX " + (name == null ? "" : Database.engrave(name) + " ") + (includeColumn ? "(`" + column + "`)" : "");
        }
    }
}
