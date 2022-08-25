package com.ptsmods.mysqlw.test;

import com.ptsmods.mysqlw.procedure.BlockBuilder;
import com.ptsmods.mysqlw.procedure.ConditionValue;
import com.ptsmods.mysqlw.procedure.stmt.loop.LeaveStmt;
import com.ptsmods.mysqlw.procedure.stmt.misc.DeclareHandlerStmt;
import com.ptsmods.mysqlw.procedure.stmt.query.InsertStmt;
import com.ptsmods.mysqlw.procedure.stmt.vars.SetStmt;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.QueryFunction;
import com.ptsmods.mysqlw.query.builder.InsertBuilder;
import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.table.ColumnDefault;
import com.ptsmods.mysqlw.table.ColumnType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.MethodName.class)
class MySQLProcedureTest {

    // We're aiming for the example here: https://dev.mysql.com/doc/refman/8.0/en/cursors.html
    @Test
    void createStatementBlock() {
        BlockBuilder b = BlockBuilder.builder();
        b.begin();

        // Declarations
        {
            b.declare("done", ColumnType.INT.struct()
                    .configure(f -> f.apply(null))
                    .setDefault(ColumnDefault.def(false))); // DECLARE done INT DEFAULT FALSE;
            b.declare("a", ColumnType.CHAR.struct()
                    .configure(f -> f.apply(16)));       // DECLARE a CHAR(16);
            b.declare(new String[] {"b, c"}, ColumnType.INT.struct()
                    .configure(f -> f.apply(null)));     // DECLARE b, c INT;

            // DECLARE cur1 CURSOR FOR SELECT id, data FROM test.t1;
            b.declareCursor("cur1", SelectBuilder.create("test.t1").select("id", "data"));
            // DECLARE cur2 CURSOR FOR SELECT i FROM test.t2;
            b.declareCursor("cur2", SelectBuilder.create("test.t2").select("i"));

            // DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = true;
            b.declareHandler(DeclareHandlerStmt.HandlerAction.CONTINUE, ConditionValue.notFound(), SetStmt.set("done", true));
            b.empty();
        }

        // Open cursors
        {
            b.openCursor("cur1");
            b.openCursor("cur2");
            b.empty();
        }

        // Actual arithmetic, loop through cursors and store data accordingly
        {
            b.loop("read_loop"); // read_loop: LOOP
            b.fetchCursor("cur1", "a", "b");
            b.fetchCursor("cur2", "c");
            b.ifBlock(block -> block.if_(QueryCondition.bool("done"), LeaveStmt.leave("read_loop")).end());
            InsertBuilder iBuilder = InsertBuilder.create("test.t3", "col1", "col2");
            b.ifBlock(block -> block
                    .if_(QueryCondition.varLess("b", "c"), InsertStmt.insert(iBuilder.clone().insert(new QueryFunction("a"), new QueryFunction("b"))))
                    .else_(InsertStmt.insert(iBuilder.clone().insert(new QueryFunction("a"), new QueryFunction("c"))))
                    .end());
            b.endLoop();
            b.empty();
        }

        // Close cursors
        {
            b.closeCursor("cur1");
            b.closeCursor("cur2");
        }

        b.end(";");
        String block = b.buildString();

        String target =
                "BEGIN\n" +
                        "  DECLARE done INT DEFAULT FALSE;\n" +
                        "  DECLARE a CHAR(16);\n" +
                        "  DECLARE b, c INT;\n" +
                        "  DECLARE cur1 CURSOR FOR SELECT `id` AS `data` FROM `test`.`t1`;\n" +
                        "  DECLARE cur2 CURSOR FOR SELECT `i` FROM `test`.`t2`;\n" +
                        "  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;\n" +
                        "\n" +
                        "  OPEN cur1;\n" +
                        "  OPEN cur2;\n" +
                        "\n" +
                        "  read_loop: LOOP\n" +
                        "    FETCH cur1 INTO a, b;\n" +
                        "    FETCH cur2 INTO c;\n" +
                        "    IF done THEN\n" +
                        "      LEAVE read_loop;\n" +
                        "    END IF;\n" +
                        "    IF b < c THEN\n" +
                        "      INSERT INTO `test`.`t3` (`col1`, `col2`) VALUES (a, b);\n" +
                        "    ELSE\n" +
                        "      INSERT INTO `test`.`t3` (`col1`, `col2`) VALUES (a, c);\n" +
                        "    END IF;\n" +
                        "  END LOOP;\n" +
                        "\n" +
                        "  CLOSE cur1;\n" +
                        "  CLOSE cur2;\n" +
                        "END;;";

        assertEquals(target, block);
    }
}
