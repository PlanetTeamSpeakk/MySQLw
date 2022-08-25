package com.ptsmods.mysqlw.procedure.stmt;

public class DelimiterStmt extends Statement {
    private final String delimiter;

    private DelimiterStmt(String delimiter) {
        this.delimiter = delimiter;
    }

    public static DelimiterStmt delimiter(String delimiter) {
        return new DelimiterStmt(delimiter);
    }

    @Override
    public String toString() {
        return "DELIMITER " + delimiter;
    }
}
