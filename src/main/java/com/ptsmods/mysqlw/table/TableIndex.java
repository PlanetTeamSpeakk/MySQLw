package com.ptsmods.mysqlw.table;

/**
 * Table indices, used to speed up queries.
 */
public class TableIndex {

    public static TableIndex index(String name, String column, TableIndexType type) {
        return new TableIndex(name, column, type);
    }

    public static TableIndex index(String column, TableIndexType type) {
        return new TableIndex(null, column, type);
    }

    private final String name, column;
    private final TableIndexType type;

    private TableIndex(String name, String column, TableIndexType type) {
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

    public TableIndexType getType() {
        return type;
    }

    @Override
    public TableIndex clone() {
        return new TableIndex(name, column, type);
    }

    @Override
    public String toString() {
        return type.toString(name, column);
    }

    public enum TableIndexType {
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
         * Indexes for {@link ColumnType#GEOMETRY GEOMETRY} objects.
         */
        SPATIAL;

        public String toString(String name, String column) {
            return (this == INDEX ? "" : name() + " ") + "INDEX " + (name == null ? "" : "`" + name + "` ") + "(`" + column + "`)";
        }
    }
}
