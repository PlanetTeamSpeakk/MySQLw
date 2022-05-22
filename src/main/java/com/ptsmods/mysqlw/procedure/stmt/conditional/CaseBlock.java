package com.ptsmods.mysqlw.procedure.stmt.conditional;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.procedure.stmt.RawStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.procedure.stmt.block.BlockLikeStatement;
import com.ptsmods.mysqlw.procedure.stmt.block.EndStmt;
import com.ptsmods.mysqlw.procedure.stmt.block.StatementBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CaseBlock extends Statement implements BlockLikeStatement {
    private final String variable;
    private final Map<Object, Statement> statements = new LinkedHashMap<>();
    private Statement defaultStmt;

    private CaseBlock(String variable) {
        this.variable = variable;
    }

    public static CaseBlock case_(String variable) {
        return new CaseBlock(variable);
    }

    public CaseBlock when(Object o, Statement stmt) {
        statements.put(o, stmt);
        return this;
    }

    public CaseBlock def(Statement stmt) {
        defaultStmt = stmt;
        return this;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int depth) {
        List<Statement> statements = new ArrayList<>();
        statements.add(RawStmt.rawOpening("CASE " + variable));
        this.statements.forEach((o, statement) -> RawStmt.raw(String.format("WHEN %s THEN %s;", Database.getAsString(o), statement)));

        if (defaultStmt != null) {
            statements.add(ElseStmt.else_());
            statements.add(defaultStmt);
        }

        statements.add(EndStmt.endCase());
        return StatementBlock.block(statements.toArray(new Statement[0])).toString(depth);
    }
}
