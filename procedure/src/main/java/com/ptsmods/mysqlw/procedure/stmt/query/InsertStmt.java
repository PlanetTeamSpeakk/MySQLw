package com.ptsmods.mysqlw.procedure.stmt.query;

import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.query.builder.InsertBuilder;

public class InsertStmt extends Statement {
    private final InsertBuilder builder;

    private InsertStmt(InsertBuilder builder) {
        this.builder = builder;
    }

    public static InsertStmt insert(InsertBuilder builder) {
        return new InsertStmt(builder);
    }

    @Override
    public String toString() {
        return builder.buildQuery();
    }
}
