package com.ptsmods.mysqlw.procedure.stmt.cursor;

import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class CloseCursorStmt extends Statement {
    private final String cursorName;

    private CloseCursorStmt(String cursorName) {
        this.cursorName = cursorName;
    }

    public static CloseCursorStmt close(String cursor) {
        return new CloseCursorStmt(cursor);
    }

    @Override
    public String toString() {
        return "CLOSE " + cursorName + ';';
    }
}
