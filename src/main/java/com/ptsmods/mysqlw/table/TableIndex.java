package com.ptsmods.mysqlw.table;

/**
 * Table indices, used to speed up queries.
 */
public class TableIndex {

    public static TableIndex index(String column, TableIndexType type) {
        return new TableIndex(column, type);
    }

    private final String column;
    private final TableIndexType type;

    private TableIndex(String column, TableIndexType type) {
        this.column = column;
        this.type = type;
    }

    @Override
    public TableIndex clone() {
        return new TableIndex(column, type);
    }

    @Override
    public String toString() {
        return type.toString(column);
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

        public String toString(String column) {
            return (this == INDEX ? "" : name() + " ") + "INDEX (`" + column + "`)";
        }
    }
}
