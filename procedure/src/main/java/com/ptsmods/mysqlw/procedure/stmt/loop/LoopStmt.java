package com.ptsmods.mysqlw.procedure.stmt.loop;

import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.OpeningStatement;

public class LoopStmt extends Statement implements OpeningStatement {
    private final String name;

    private LoopStmt(String name) {
        this.name = name;
    }

    public static LoopStmt loop(String name) {
        return new LoopStmt(name);
    }

    @Override
    public String toString() {
        return name + ": LOOP";
    }
}
