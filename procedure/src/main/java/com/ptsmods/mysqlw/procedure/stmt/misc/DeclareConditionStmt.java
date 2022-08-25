package com.ptsmods.mysqlw.procedure.stmt.misc;

import com.ptsmods.mysqlw.procedure.ConditionValue;
import com.ptsmods.mysqlw.procedure.stmt.DeclaringStmt;
import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class DeclareConditionStmt extends Statement implements DeclaringStmt {
    private final String name;
    private final ConditionValue conditionValue;

    private DeclareConditionStmt(String name, ConditionValue conditionValue) {
        if (conditionValue.getType() != ConditionValue.Type.SQL_ERROR && conditionValue.getType() != ConditionValue.Type.SQL_STATE)
            throw new IllegalArgumentException("ConditionValue must be SQL_ERROR or SQL_STATE.");

        this.name = name;
        this.conditionValue = conditionValue;
    }

    public static DeclareConditionStmt declareCondition(String name, ConditionValue conditionValue) {
        return new DeclareConditionStmt(name, conditionValue);
    }

    @Override
    public String toString() {
        return String.format("DECLARE %s CONDITION FOR %s;", name, conditionValue);
    }
}
