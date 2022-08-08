package com.ptsmods.mysqlw.table;

/**
 * Certain attributes the type of a column may have.
 */
public enum ColumnAttributes {
    /**
     * Marks any String type as a binary string.
     */
    BINARY,
    /**
     * Store any number type as unsigned.
     */
    UNSIGNED,
    /**
     * Same as {@link #UNSIGNED} but also fills the number with zeros to fit the length.
     */
    UNSIGNED_ZEROFILL("UNSIGNED ZEROFILL"),
    /**
     * Same as {@link #UNSIGNED} but also fills the number with zeros to fit the length.
     * @deprecated This was a typo, but the value has always been correct. Please use {@link #UNSIGNED_ZEROFILL} instead.
     */
    @Deprecated
    UNSIGED_ZEROFILL("UNSIGNED ZEROFILL"),
    /**
     * Store the current {@link ColumnType#TIMESTAMP TIMESTAMP} in this column whenever the row gets updated.
     */
    UPDATE_CURRENT_TIMESTAMP("on update CURRENT_TIMESTAMP");

    private final String s;

    ColumnAttributes() {
        this(null);
    }

    ColumnAttributes(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return s == null ? name() : s;
    }
}
