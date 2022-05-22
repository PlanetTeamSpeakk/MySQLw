package com.ptsmods.mysqlw.procedure.stmt.vars;

import com.ptsmods.mysqlw.procedure.stmt.DeclaringStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.table.ColumnStructure;

public class DeclareStmt extends Statement implements DeclaringStmt {
    private final String[] varNames;
    private final ColumnStructure<?> structure;

    private DeclareStmt(String[] varNames, ColumnStructure<?> structure) {
        this.varNames = varNames.clone();
        this.structure = structure;
    }

    public static DeclareStmt declare(String varName, ColumnStructure<?> type) {
        return new DeclareStmt(new String[] {varName}, type);
    }

    public static DeclareStmt declare(String[] varNames, ColumnStructure<?> type) {
        return new DeclareStmt(varNames, type);
    }

    @Override
    public String toString() {
        return String.format("DECLARE %s %s%s;", String.join(", ", varNames), structure.getTypeString(), structure.getDefValue() == null ? "" : " DEFAULT " + structure.getDefValue().getDefString());
    }
}
