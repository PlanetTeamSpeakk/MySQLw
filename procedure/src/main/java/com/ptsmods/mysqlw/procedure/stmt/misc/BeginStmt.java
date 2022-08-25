package com.ptsmods.mysqlw.procedure.stmt.misc;

import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.OpeningStatement;

public class BeginStmt extends Statement implements OpeningStatement {

    private BeginStmt() {}

    public static BeginStmt begin() {
        return new BeginStmt();
    }

    @Override
    public String toString() {
        return "BEGIN";
    }
}
