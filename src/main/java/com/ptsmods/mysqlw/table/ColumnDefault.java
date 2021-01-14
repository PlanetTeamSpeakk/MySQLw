package com.ptsmods.mysqlw.table;

/**
 * The default value of a column.
 */
public enum ColumnDefault {
    /**
     * Sets the default value of a column to be null, this is the default behaviour.
     * When using this in a {@link ColumnStructure}, the column will always allow null values.
     */
    NULL,
    /**
     * Whenever a new row is made, the value of this column is set to the current {@link ColumnType#TIMESTAMP TIMESTAMP}.
     */
    CURRENT_TIMESTAMP;
}
