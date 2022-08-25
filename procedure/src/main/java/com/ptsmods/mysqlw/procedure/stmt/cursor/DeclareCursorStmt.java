package com.ptsmods.mysqlw.procedure.stmt.cursor;

import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.procedure.stmt.DeclaringStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class DeclareCursorStmt extends Statement implements DeclaringStmt {
    private final String name;
    private final SelectBuilder query;

    private DeclareCursorStmt(String name, SelectBuilder query) {
        this.name = name;
        this.query = query;
    }

    public static DeclareCursorStmt declareCursor(String name, SelectBuilder query) {
        return new DeclareCursorStmt(name, query);
    }

    @Override
    public String toString() {
        return String.format("DECLARE %s CURSOR FOR %s;", name, query.buildQuery());
    }
}
