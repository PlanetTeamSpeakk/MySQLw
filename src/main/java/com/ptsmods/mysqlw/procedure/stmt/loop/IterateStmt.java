package com.ptsmods.mysqlw.procedure.stmt.loop;

import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class IterateStmt extends Statement {
    private final String loop;

    private IterateStmt(String loop) {
        this.loop = loop;
    }

    public static IterateStmt iterate(String loop) {
        return new IterateStmt(loop);
    }

    @Override
    public String toString() {
        return String.format("ITERATE %s;", loop);
    }
}
