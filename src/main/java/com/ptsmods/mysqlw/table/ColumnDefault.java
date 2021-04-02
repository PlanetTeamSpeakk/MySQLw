package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryFunction;

/**
 * The default value of a column.
 */
public class ColumnDefault {
    /**
     * Sets the default value of a column to be null, this is the default behaviour.
     * When using this in a {@link ColumnStructure}, the column will always allow null values.
     */
    public static final ColumnDefault NULL = new ColumnDefault(new QueryFunction("NULL"));
    /**
     * Whenever a new row is made, the value of this column is set to the current {@link ColumnType#TIMESTAMP TIMESTAMP}.
     */
    public static final ColumnDefault CURRENT_TIMESTAMP = new ColumnDefault(new QueryFunction("CURRENT_TIMESTAMP"));

    /**
     * Create a default value for a column.
     * @param def The value to use as a default.
     * @return A new ColumnDefault with the given value as a default value.
     */
    public static ColumnDefault def(Object def) {
        return new ColumnDefault(def);
    }

    private final Object def;

    private ColumnDefault(Object def) {
        this.def = def;
    }

    /**
     * @return The default value of this ColumnDefault.
     * @see #getDefString()
     */
    public Object getDef() {
        return def;
    }

    /**
     * @return The String representation of the default value of this ColumnDefault.
     * @see #getDef()
     */
    public String getDefString() {
        return Database.getAsString(getDef());
    }
}
