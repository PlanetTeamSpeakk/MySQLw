package com.ptsmods.mysqlw.procedure.stmt.vars;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.procedure.stmt.DeclaringStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;
import com.ptsmods.mysqlw.table.ColumnStructure;

public class DeclareStmt extends Statement implements DeclaringStmt {
    private final Database.RDBMS type;
    private final String[] varNames;
    private final ColumnStructure<?> structure;

    private DeclareStmt(Database.RDBMS dbType, String[] varNames, ColumnStructure<?> structure) {
        this.type = dbType;
        this.varNames = varNames.clone();
        this.structure = structure;
    }

    public static DeclareStmt declare(Database.RDBMS dbType, String varName, ColumnStructure<?> type) {
        return new DeclareStmt(dbType, new String[] {varName}, type);
    }

    public static DeclareStmt declare(Database.RDBMS dbType, String[] varNames, ColumnStructure<?> type) {
        return new DeclareStmt(dbType, varNames, type);
    }

    @Override
    public String toString() {
        return String.format("DECLARE %s %s%s;", String.join(", ", varNames), structure.buildTypeString(type),
                structure.getDefValue() == null ? "" : " DEFAULT " + structure.getDefValue().getDefString());
    }
}
