package com.ptsmods.mysqlw.procedure.stmt.cursor;

import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class FetchCursorStmt extends Statement {
    private final String cursor;
    private final String[] variables;

    private FetchCursorStmt(String cursor, String... variables) {
        this.cursor = cursor;
        this.variables = variables.clone();
    }

    public static FetchCursorStmt fetch(String cursor, String... variables) {
        return new FetchCursorStmt(cursor, variables);
    }

    @Override
    public String toString() {
        return String.format("FETCH %s INTO %s;", cursor, String.join(", ", variables));
    }
}
