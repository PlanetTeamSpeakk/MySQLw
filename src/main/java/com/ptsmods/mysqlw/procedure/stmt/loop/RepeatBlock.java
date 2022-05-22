package com.ptsmods.mysqlw.procedure.stmt.loop;

import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.procedure.BlockBuilder;
import com.ptsmods.mysqlw.procedure.stmt.RawStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.BlockLikeStatement;
import com.ptsmods.mysqlw.procedure.stmt.block.StatementBlock;

public class RepeatBlock extends Statement implements BlockLikeStatement {
    private final QueryCondition condition;
    private final Statement statement;

    private RepeatBlock(QueryCondition condition, Statement statement) {
        this.condition = condition;
        this.statement = statement;
    }

    public static RepeatBlock repeat(QueryCondition condition, Statement statement) {
        return new RepeatBlock(condition, statement);
    }

    public static RepeatBlock repeat(QueryCondition condition, BlockBuilder builder) {
        return new RepeatBlock(condition, builder.buildBlock());
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int depth) {
        return StatementBlock.block(
                RawStmt.rawOpening("REPEAT"),
                statement,
                RawStmt.rawClosing("UNTIL " + condition + " END REPEAT;")
        ).toString(depth);
    }
}
