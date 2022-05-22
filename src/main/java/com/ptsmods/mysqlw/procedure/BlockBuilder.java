package com.ptsmods.mysqlw.procedure;

import com.ptsmods.mysqlw.procedure.stmt.DeclaringStmt;
import com.ptsmods.mysqlw.procedure.stmt.RawStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.EndStmt;
import com.ptsmods.mysqlw.procedure.stmt.block.StatementBlock;
import com.ptsmods.mysqlw.procedure.stmt.conditional.CaseBlock;
import com.ptsmods.mysqlw.procedure.stmt.conditional.IfBlock;
import com.ptsmods.mysqlw.procedure.stmt.cursor.CloseCursorStmt;
import com.ptsmods.mysqlw.procedure.stmt.cursor.DeclareCursorStmt;
import com.ptsmods.mysqlw.procedure.stmt.cursor.FetchCursorStmt;
import com.ptsmods.mysqlw.procedure.stmt.cursor.OpenCursorStmt;
import com.ptsmods.mysqlw.procedure.stmt.loop.*;
import com.ptsmods.mysqlw.procedure.stmt.misc.BeginStmt;
import com.ptsmods.mysqlw.procedure.stmt.misc.DeclareConditionStmt;
import com.ptsmods.mysqlw.procedure.stmt.misc.DeclareHandlerStmt;
import com.ptsmods.mysqlw.procedure.stmt.vars.DeclareStmt;
import com.ptsmods.mysqlw.procedure.stmt.vars.SetStmt;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.table.ColumnStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BlockBuilder {
    private final List<Statement> statements = new ArrayList<>();

    private BlockBuilder() {}

    public static BlockBuilder builder() {
        return new BlockBuilder();
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public BlockBuilder stmt(int index, Statement statement) {
        if (statement instanceof DeclaringStmt && !statements.stream().allMatch(stmt -> stmt instanceof DeclaringStmt || stmt instanceof BeginStmt))
            throw new IllegalArgumentException("All declaring statements must be at the top of the block.");

        statements.add(index, statement);
        return this;
    }

    public BlockBuilder stmt(Statement statement) {
        return stmt(statements.size(), statement);
    }

    public BlockBuilder stmts(Statement... statements) {
        for (Statement statement : statements) stmt(statement);
        return this;
    }

    public BlockBuilder stmts(Iterable<Statement> statements) {
        for (Statement statement : statements) stmt(statement);
        return this;
    }

    public BlockBuilder raw(String statement) {
        return stmt(RawStmt.raw(statement));
    }

    public BlockBuilder begin() {
        return stmt(BeginStmt.begin());
    }

    public BlockBuilder declare(String varName, ColumnStructure<?> type) {
        return stmt(DeclareStmt.declare(varName, type));
    }

    public BlockBuilder declare(String[] varNames, ColumnStructure<?> type) {
        return stmt(DeclareStmt.declare(varNames, type));
    }

    public BlockBuilder set(String variable, Object value) {
        return stmt(SetStmt.set(variable, value));
    }

    public BlockBuilder set(String variable, SelectBuilder query) {
        return stmt(SetStmt.set(variable, query));
    }

    public BlockBuilder declareHandler(DeclareHandlerStmt.HandlerAction action, ConditionValue conditionValue, Statement statement) {
        return stmt(DeclareHandlerStmt.declareHandler(action, conditionValue, statement));
    }

    public BlockBuilder declareCondition(String name, ConditionValue conditionValue) {
        return stmt(DeclareConditionStmt.declareCondition(name, conditionValue));
    }

    public BlockBuilder declareCursor(String name, SelectBuilder query) {
        return stmt(DeclareCursorStmt.declareCursor(name, query));
    }

    public BlockBuilder openCursor(String cursor) {
        return stmt(OpenCursorStmt.open(cursor));
    }

    public BlockBuilder fetchCursor(String cursor, String... columns) {
        return stmt(FetchCursorStmt.fetch(cursor, columns));
    }

    public BlockBuilder closeCursor(String cursor) {
        return stmt(CloseCursorStmt.close(cursor));
    }

    public BlockBuilder loop(String name) {
        return stmt(LoopStmt.loop(name));
    }

    public BlockBuilder repeat(QueryCondition condition, Statement statement) {
        return stmt(RepeatBlock.repeat(condition, statement));
    }

    public BlockBuilder repeat(QueryCondition condition, BlockBuilder builder) {
        return stmt(RepeatBlock.repeat(condition, builder));
    }

    public BlockBuilder while_(QueryCondition condition) {
        return stmt(WhileStmt.while_(condition));
    }

    public BlockBuilder iterate(String loop) {
        return stmt(IterateStmt.iterate(loop));
    }

    public BlockBuilder leave(String loop) {
        return stmt(LeaveStmt.leave(loop));
    }

    public BlockBuilder case_(String variable, Consumer<CaseBlock> populator) {
        CaseBlock block = CaseBlock.case_(variable);
        populator.accept(block);
        return stmt(block);
    }

    public BlockBuilder ifBlock(Consumer<IfBlock> populator) {
        IfBlock block = IfBlock.block();
        populator.accept(block);
        return stmt(block);
    }

    public BlockBuilder endLoop() {
        return stmt(EndStmt.endLoop());
    }

    public BlockBuilder endWhile() {
        return stmt(EndStmt.endWhile());
    }

    public BlockBuilder endCase() {
        return stmt(EndStmt.endCase());
    }

    public BlockBuilder end(String delimiter) {
        return stmt(EndStmt.end(delimiter));
    }

    public BlockBuilder empty() {
        return stmt(EmptyStmt.empty());
    }

    public StatementBlock buildBlock() {
        return StatementBlock.block(statements.toArray(new Statement[0]));
    }

    public String buildString() {
        return buildBlock().toString();
    }
}
