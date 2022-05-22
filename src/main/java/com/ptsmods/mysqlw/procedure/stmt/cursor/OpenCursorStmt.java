package com.ptsmods.mysqlw.procedure.stmt.cursor;

import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class OpenCursorStmt extends Statement {
    private final String cursorName;

    private OpenCursorStmt(String cursorName) {
        this.cursorName = cursorName;
    }

    public static OpenCursorStmt open(String cursor) {
        return new OpenCursorStmt(cursor);
    }

    @Override
    public String toString() {
        return String.format("OPEN %s;", cursorName);
    }
}
