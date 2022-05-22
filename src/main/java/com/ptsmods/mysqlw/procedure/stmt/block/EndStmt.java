package com.ptsmods.mysqlw.procedure.stmt.block;

import com.ptsmods.mysqlw.procedure.stmt.Statement;

/**
 * Statement ending a block of statements.
 */
public class EndStmt extends Statement implements ClosingStatement {
    private final String type;

    private EndStmt(String type) {
        this.type = type;
    }

    /**
     * End an if-block
     * @return An EndStmt
     */
    public static EndStmt endIf() {
        return new EndStmt(" IF");
    }

    /**
     * End a loop-block
     * @return An EndStmt
     */
    public static EndStmt endLoop() {
        return new EndStmt(" LOOP");
    }

    /**
     * End a while-block
     * @return An EndStmt
     */
    public static EndStmt endWhile() {
        return new EndStmt(" WHILE");
    }

    /**
     * End a case-block
     * @return An EndStmt
     */
    public static EndStmt endCase() {
        return new EndStmt(" CASE");
    }

    /**
     * End a trigger/procedure
     * @param delimiter The delimiter to append to the statement
     * @return An EndStmt
     */
    public static EndStmt end(String delimiter) {
        return new EndStmt(delimiter);
    }

    @Override
    public String toString() {
        return String.format("END%s;", type);
    }
}
