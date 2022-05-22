package com.ptsmods.mysqlw.procedure.stmt.vars;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.procedure.stmt.Statement;

import java.util.function.Supplier;

public class SetStmt extends Statement {
    private final String variable;
    private final Supplier<String> expression;

    private SetStmt(String variable, Supplier<String> expression) {
        this.variable = variable;
        this.expression = expression;
    }

    public static SetStmt set(String variable, Object value) {
        return new SetStmt(variable, () -> Database.getAsString(value));
    }

    public static SetStmt set(String variable, SelectBuilder query) {
        if (query.getColumns().size() != 1) throw new IllegalArgumentException("The select query must select one column or function.");
        return new SetStmt(variable, () -> String.format("(%s)", query.buildQuery()));
    }

    @Override
    public String toString() {
        return String.format("SET %s = %s;", variable, expression.get());
    }
}
