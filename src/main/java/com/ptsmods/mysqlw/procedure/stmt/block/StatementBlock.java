package com.ptsmods.mysqlw.procedure.stmt.block;

import com.ptsmods.mysqlw.procedure.BlockBuilder;
import com.ptsmods.mysqlw.procedure.stmt.Statement;

import java.util.function.Function;
import java.util.stream.StreamSupport;

public class StatementBlock extends Statement implements BlockLikeStatement {
    private final Statement[] statements;
    private final Function<Integer, String> stringRes;

    private StatementBlock(Statement... statements) {
        this.statements = statements.clone();
        stringRes = depth -> {
            StringBuilder builder = new StringBuilder();

            for (Statement statement : this.statements) {
                if (statement instanceof ClosingStatement) depth--;

                if (!(statement instanceof BlockLikeStatement)) for (int i = 0; i < depth; i++) builder.append("  ");
                builder.append(statement instanceof BlockLikeStatement ? ((BlockLikeStatement) statement).toString(depth) : statement).append('\n');

                if (statement instanceof OpeningStatement) depth++;
            }

            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        };
    }

    public static StatementBlock block(Statement... statements) {
        return new StatementBlock(statements);
    }

    public static StatementBlock block(Iterable<Statement> statements) {
        return new StatementBlock(StreamSupport.stream(statements.spliterator(), false).toArray(Statement[]::new));
    }

    public BlockBuilder toBuilder() {
        return BlockBuilder.builder().stmts(statements);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int depth) {
        return stringRes.apply(depth);
    }
}
