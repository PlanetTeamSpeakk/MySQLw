package com.ptsmods.mysqlw.procedure.stmt.loop;

import com.ptsmods.mysqlw.procedure.stmt.Statement;

public class LeaveStmt extends Statement {
    private final String loop;

    private LeaveStmt(String loop) {
        this.loop = loop;
    }

    public static LeaveStmt leave(String loop) {
        return new LeaveStmt(loop);
    }

    @Override
    public String toString() {
        return String.format("LEAVE %s;", loop);
    }
}
