package com.ptsmods.mysqlw.procedure.stmt.loop;

import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.OpeningStatement;

public class WhileStmt extends Statement implements OpeningStatement {
    private final QueryCondition condition;

    private WhileStmt(QueryCondition condition) {
        this.condition = condition;
    }

    public static WhileStmt while_(QueryCondition condition) {
        return new WhileStmt(condition);
    }

    @Override
    public String toString() {
        return String.format("WHILE %s DO", condition);
    }
}
