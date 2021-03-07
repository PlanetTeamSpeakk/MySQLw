package com.ptsmods.mysqlw;

import java.sql.SQLException;

/**
 * An Exception class to wrap {@link SQLException}s to be thrown silently.
 */
public class SilentSQLException extends RuntimeException {

    private final SQLException parent;

    public SilentSQLException(SQLException parent) {
        super(parent.getMessage(), parent.getCause());
        this.parent = parent;
    }

    public SQLException getParent() {
        return parent;
    }

    public String getSQLState() {
        return parent.getSQLState();
    }

    public int getErrorCode() {
        return parent.getErrorCode();
    }
}
