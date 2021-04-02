package com.ptsmods.mysqlw.test;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.QueryFunction;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TablePreset;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DescribeTest {

    @Test
    void describe() throws SQLException {
        //Database db = assertDoesNotThrow(() -> Database.connect("localhost", 3306, "test", "root", null));
        Database db = assertDoesNotThrow(() -> Database.connect(new File("sqlite.db")));
        ResultSet set = db.selectRaw("list_testlist", "*", QueryCondition.func(new QueryFunction("0=1")), null, null);
        ResultSetMetaData md = set.getMetaData();
        TablePreset preset = TablePreset.create("list_testlist");
        for (int i = 1; i <= md.getColumnCount(); i++) {
            preset.putColumn(md.getColumnName(i), ColumnType.types.get(md.getColumnTypeName(i).replace("INTEGER", "INT")).createStructure().setTypeString(md.getColumnTypeName(i)).setAutoIncrement(md.isAutoIncrement(i)).setNullAllowed(md.isNullable(i) == ResultSetMetaData.columnNullable));
        }
        System.out.println(preset.buildQuery(db.getType()));
        //System.out.println(db.select("information_schema.COLUMNS", "*", QueryConditions.create().and(QueryCondition.equals("TABLE_NAME", "list_testlist")).and(QueryCondition.equals("TABLE_SCHEMA", db.getName())), null));
        System.out.println(db.getCreateQuery("list_testlist"));
        set.close();
    }

}
