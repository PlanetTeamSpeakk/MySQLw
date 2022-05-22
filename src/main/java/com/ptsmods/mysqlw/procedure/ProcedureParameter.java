package com.ptsmods.mysqlw.procedure;

import com.ptsmods.mysqlw.table.ColumnStructure;

public class ProcedureParameter {
    private final String name;
    private final ColumnStructure<?> type;
    private boolean in = false;
    private boolean out = false;

    private ProcedureParameter(String name, ColumnStructure<?> type) {
        this.name = name;
        this.type = type;
    }

    public static ProcedureParameter parameter(String name, ColumnStructure<?> type) {
        return new ProcedureParameter(name, type);
    }

    public ProcedureParameter in() {
        in = !in;
        return this;
    }

    public ProcedureParameter out() {
        out = !out;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", in ? out ? "INOUT " : "IN " : out ? "OUT " : "", name, type.getTypeString());
    }
}
