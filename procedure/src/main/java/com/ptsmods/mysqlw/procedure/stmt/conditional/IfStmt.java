package com.ptsmods.mysqlw.procedure.stmt.conditional;

import com.ptsmods.mysqlw.procedure.stmt.block.OpeningStatement;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class IfStmt extends Statement implements OpeningStatement {
    private final QueryCondition condition;

    private IfStmt(QueryCondition condition) {
        this.condition = condition;
    }

    public static IfStmt if_(QueryCondition condition) {
        return new IfStmt(condition);
    }

    @Override
    public String toString() {
        return String.format("IF %s THEN", condition);
    }
}
