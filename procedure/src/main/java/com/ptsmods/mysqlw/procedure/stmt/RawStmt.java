package com.ptsmods.mysqlw.procedure.stmt;

import com.ptsmods.mysqlw.procedure.stmt.block.ClosingStatement;
import com.ptsmods.mysqlw.procedure.stmt.block.OpeningStatement;

public class RawStmt extends Statement {
    private final String statement;

    private RawStmt(String statement) {
        this.statement = statement;
    }

    public static RawStmt raw(String statement) {
        return new RawStmt(statement);
    }

    public static RawStmt rawOpening(String statement) {
        return new RawOpeningStmt(statement);
    }

    public static RawStmt rawClosing(String statement) {
        return new RawClosingStmt(statement);
    }

    public static RawStmt rawClosingOpening(String statement) {
        return new RawClosingOpeningStmt(statement);
    }

    @Override
    public String toString() {
        return statement;
    }

    private static class RawOpeningStmt extends RawStmt implements OpeningStatement {
        private RawOpeningStmt(String statement) {
            super(statement);
        }
    }

    private static class RawClosingStmt extends RawStmt implements ClosingStatement {
        private RawClosingStmt(String statement) {
            super(statement);
        }
    }

    private static class RawClosingOpeningStmt extends RawStmt implements ClosingStatement, OpeningStatement {
        private RawClosingOpeningStmt(String statement) {
            super(statement);
        }
    }
}
