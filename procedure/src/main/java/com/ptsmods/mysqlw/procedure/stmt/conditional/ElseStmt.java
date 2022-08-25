package com.ptsmods.mysqlw.procedure.stmt.conditional;

import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.ClosingStatement;
import com.ptsmods.mysqlw.procedure.stmt.block.OpeningStatement;

// Closes a block and opens a new one
public class ElseStmt extends Statement implements ClosingStatement, OpeningStatement {

    private ElseStmt() {}

    public static ElseStmt else_() {
        return new ElseStmt();
    }

    @Override
    public String toString() {
        return "ELSE";
    }
}
