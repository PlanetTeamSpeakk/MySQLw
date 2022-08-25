package com.ptsmods.mysqlw.procedure;

import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.BlockLikeStatement;

// Implements BlockLikeStatement so no whitespace is added by StatementBlock
public class EmptyStmt extends Statement implements BlockLikeStatement {

    private EmptyStmt() {}

    public static EmptyStmt empty() {
        return new EmptyStmt();
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public String toString(int depth) {
        return "";
    }
}
