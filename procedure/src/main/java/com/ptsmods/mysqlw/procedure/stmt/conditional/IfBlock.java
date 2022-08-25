package com.ptsmods.mysqlw.procedure.stmt.conditional;

import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.procedure.BlockBuilder;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.BlockLikeStatement;
import com.ptsmods.mysqlw.procedure.stmt.block.EndStmt;
import com.ptsmods.mysqlw.procedure.stmt.block.StatementBlock;

import java.util.ArrayList;
import java.util.List;

public class IfBlock extends Statement implements BlockLikeStatement {
    private final List<Statement> statements = new ArrayList<>();
    private Boolean open = Boolean.FALSE;

    private IfBlock() {}

    public static IfBlock block() {
        return new IfBlock();
    }

    public IfBlock if_(QueryCondition condition, Statement statement) {
        if (open != Boolean.FALSE) throw new IllegalStateException("If-block has already been opened.");

        statements.add(IfStmt.if_(condition));
        statements.add(statement);
        open = Boolean.TRUE;
        return this;
    }

    public IfBlock if_(QueryCondition condition, BlockBuilder builder) {
        return if_(condition, builder.buildBlock());
    }

    public IfBlock elseIf(QueryCondition condition, Statement statement) {
        if (open != Boolean.TRUE) throw new IllegalStateException("If-block has either not yet been opened or is already closed.");

        statements.add(ElseIfStmt.elseIf(condition));
        statements.add(statement);
        return this;
    }

    public IfBlock elseIf(QueryCondition condition, BlockBuilder builder) {
        return elseIf(condition, builder.buildBlock());
    }

    public IfBlock else_(Statement statement) {
        if (open != Boolean.TRUE) throw new IllegalStateException("If-block has either not yet been opened or is already closed.");

        statements.add(ElseStmt.else_());
        statements.add(statement);
        return this;
    }

    public IfBlock else_(BlockBuilder builder) {
        return else_(builder.buildBlock());
    }

    public IfBlock end() {
        if (open != Boolean.TRUE) throw new IllegalStateException("If-block has either not yet been opened or is already closed.");

        statements.add(EndStmt.endIf());
        open = null;
        return this;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int depth) {
        return StatementBlock.block(statements).toString(depth);
    }
}
