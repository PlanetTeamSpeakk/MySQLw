package com.ptsmods.mysqlw.procedure.stmt.misc;

import com.ptsmods.mysqlw.procedure.ConditionValue;
import com.ptsmods.mysqlw.procedure.stmt.DeclaringStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.BlockLikeStatement;

public class DeclareHandlerStmt extends Statement implements DeclaringStmt, BlockLikeStatement {
    private final HandlerAction action;
    private final ConditionValue conditionValue;
    private final Statement statement;

    private DeclareHandlerStmt(HandlerAction action, ConditionValue conditionValue, Statement statement) {
        this.action = action;
        this.conditionValue = conditionValue;
        this.statement = statement;
    }

    public static DeclareHandlerStmt declareHandler(HandlerAction action, ConditionValue conditionValue, Statement statement) {
        return new DeclareHandlerStmt(action, conditionValue, statement);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int depth) {
        StringBuilder whitespace = new StringBuilder();
        for (int i = 0; i < depth; i++) whitespace.append("  ");

        boolean blockLike = statement instanceof BlockLikeStatement;
        return String.format(whitespace + "DECLARE %s HANDLER FOR %s %s%s", action.name(), conditionValue.toString(),
                blockLike ? "\n" : "", blockLike ? ((BlockLikeStatement) statement).toString(depth + 1) : statement); // Semicolon included in the statement
    }

    public enum HandlerAction {
        CONTINUE, EXIT, UNDO
    }
}
