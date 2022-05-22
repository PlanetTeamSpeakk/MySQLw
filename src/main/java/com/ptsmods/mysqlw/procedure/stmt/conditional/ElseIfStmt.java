package com.ptsmods.mysqlw.procedure.stmt.conditional;

import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class ElseIfStmt extends Statement {
    private final QueryCondition condition;

    private ElseIfStmt(QueryCondition condition) {
        this.condition = condition;
    }

    public static ElseIfStmt elseIf(QueryCondition condition) {
        return new ElseIfStmt(condition);
    }

    @Override
    public String toString() {
        return String.format("ELSEIF %s THEN", condition);
    }
}
