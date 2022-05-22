package com.ptsmods.mysqlw.procedure.stmt.query;

import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.query.builder.SelectBuilder;

public class SelectStmt extends Statement {
    private final SelectBuilder builder;

    private SelectStmt(SelectBuilder builder) {
        this.builder = builder;
    }

    public static SelectStmt select(SelectBuilder builder) {
        return new SelectStmt(builder);
    }

    @Override
    public String toString() {
        return builder.buildQuery();
    }
}
