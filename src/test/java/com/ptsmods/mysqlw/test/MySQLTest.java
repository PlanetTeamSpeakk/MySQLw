package com.ptsmods.mysqlw.test;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.collection.DbList;
import com.ptsmods.mysqlw.collection.DbMap;
import com.ptsmods.mysqlw.collection.DbSet;
import com.ptsmods.mysqlw.procedure.BlockBuilder;
import com.ptsmods.mysqlw.procedure.ConditionValue;
import com.ptsmods.mysqlw.procedure.stmt.loop.LeaveStmt;
import com.ptsmods.mysqlw.procedure.stmt.misc.DeclareHandlerStmt;
import com.ptsmods.mysqlw.procedure.stmt.query.InsertStmt;
import com.ptsmods.mysqlw.procedure.stmt.vars.SetStmt;
import com.ptsmods.mysqlw.query.*;
import com.ptsmods.mysqlw.query.builder.InsertBuilder;
import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.table.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL")
@TestMethodOrder(MethodOrderer.MethodName.class)
class MySQLTest {
    private static final UUID testId = UUID.nameUUIDFromBytes("MySQLw".getBytes(StandardCharsets.UTF_8));
    private static Database db = null;

    Database getDb() throws SQLException {
        if (db == null) {
            db = Database.connect("localhost", 3306, "test", "root", null);

            TablePreset.create("testtable")
                    .putColumn("keyword", ColumnType.VARCHAR.struct()
                            .configure(sup -> sup.apply(255))
                            .setPrimary())
                    .putColumn("value", ColumnType.TEXT.struct())
                    .create(db);

            TablePreset.create("join_test_1")
                    .putColumn("id", ColumnType.INT.struct()
                            .setAutoIncrement()
                            .setPrimary()
                            .setNonNull())
                    .putColumn("value1", ColumnType.TEXT.struct())
                    .create(db);

            TablePreset.create("join_test_2")
                    .putColumn("id", ColumnType.INT.struct()
                            .setAutoIncrement()
                            .setPrimary()
                            .setNonNull())
                    .putColumn("value2", ColumnType.UUID.struct())
                    .create(db);

            if (db.count("testtable", "*", null) == 0)
                db.insertBuilder("testtable", "keyword", "value")
                        .insert("key1", "val1")
                        .insert("key2", "val2")
                        .execute();

            if (db.count("join_test_1", "*", null) == 0) {
                db.insert("join_test_1", "value1", "Value from table 1");
                db.insert("join_test_2", "value2", testId);
            }

            db.setLogging(false);
        }

        return db;
    }

    @Test
    void _loadConnector() {
        assertDoesNotThrow(() -> Database.loadConnector(Database.RDBMS.MySQL, "8.0.23", new File("mysql-connector.jar"), true));
    }

    @Test
    void connect() {
        assertDoesNotThrow(this::getDb);
    }

    @Test
    void testMap() throws SQLException {
        DbMap<String, Integer> map = DbMap.getMap(getDb(), "testmap", String.class, Integer.class);
        map.clear();
        assertEquals(0, map.size());
        map.put("testkey", 42);
        assertTrue(map.containsKey("testkey"));
        assertEquals(42, map.get("testkey"));
        assertEquals(1, map.size());
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    void testList() throws SQLException {
        DbList<String> list = DbList.getList(getDb(), "testlist", String.class);
        assertEquals(0, list.size());
        list.add("Hello");
        assertEquals(1, list.size());
        assertEquals("Hello", list.get(0));
        assertTrue(list.contains("Hello"));
        list.addAll(Arrays.asList("test", "test2"));
        assertTrue(list.containsAll(Arrays.asList("test", "test2")));
        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    void testSet() throws SQLException {
        DbSet<String> set = DbSet.getSet(getDb(), "testset", String.class);
        assertEquals(0, set.size());
        set.add("hey");
        set.add("Hello");
        assertEquals(2, set.size());
        assertTrue(set.contains("hey"));
        set.addAll(Arrays.asList("test", "test2"));
        assertTrue(set.containsAll(Arrays.asList("test", "test2")));
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    void count() throws SQLException {
        assertEquals(2, getDb().count("testtable", "*", null));
    }

    @Test
    void truncate() throws SQLException {
        getDb().truncate("testtable");
        assertEquals(0, getDb().count("testtable", "*", null));
        getDb().insert("testtable", new String[] {"keyword", "value"}, Arrays.asList(new Object[] {"key1", "val1"}, new Object[] {"key2", "val2"}));
    }

    @Test
    void delete() throws SQLException {
        assertEquals(1, getDb().delete("testtable", QueryCondition.equals("keyword", "key2")));
        getDb().insert("testtable", new String[] {"keyword", "value"}, new Object[] {"key2", "val2"});
    }

    @Test
    void select() throws SQLException {
        assertEquals(2, getDb().select("testtable", "*", QueryCondition.equals("keyword", "key2").or(QueryCondition.equals("value", "val1")), null, null).size());
        assertEquals(1, getDb().select("testtable", "*", QueryCondition.equals("keyword", "key2"), null, null).size());
    }

    @Test
    void insert() throws SQLException {
        assertEquals(0, getDb().select("testtable", "*", QueryCondition.equals("keyword", "key3"), null, null).size());
        getDb().insert("testtable", new String[] {"keyword", "value"}, new Object[] {"key3", "val3"});
        assertEquals(1, getDb().select("testtable", "*", QueryCondition.equals("keyword", "key3"), null, null).size());
        assertEquals(0, getDb().select("testtable", "*", QueryConditions.create(QueryCondition.equals("keyword", "key4")).or(QueryCondition.equals("keyword", "key5")), null, null).size());
        getDb().insert("testtable", new String[] {"keyword", "value"}, Arrays.asList(new Object[] {"key4", "val4"}, new Object[] {"key5", "val5"}));
        assertEquals(2, getDb().select("testtable", "*", QueryConditions.create(QueryCondition.equals("keyword", "key4")).or(QueryCondition.equals("keyword", "key5")), null, null).size());
        QueryCondition condition = QueryConditions.create(QueryCondition.equals("keyword", "key3")).or(QueryCondition.equals("keyword", "key4")).or(QueryCondition.equals("keyword", "key5"));
        getDb().delete("testtable", condition);
    }

//    @Test
//    void insertDuplicate() throws SQLException {
//        assertEquals("val2", getDb().select("testtable", "value", QueryCondition.equals("keyword", "key2"), null, null).get(0).get("value"));
//        assertEquals(1, getDb().insertUpdate("testtable", new String[] {"keyword", "value"}, new Object[] {"key2", "val2"}, Database.singletonMap("value", "val6"), "keyword"));
//        assertEquals("val6", getDb().select("testtable", "value", QueryCondition.equals("keyword", "key2"), null, null).get(0).get("value"));
//        assertEquals(1, getDb().insertUpdate("testtable", new String[] {"keyword", "value"}, new Object[] {"key2", "val2"}, Database.singletonMap("value", "val2"), "keyword"));
//    }

    @Test
    void update() throws SQLException {
        assertEquals("val2", getDb().select("testtable", "value", QueryCondition.equals("keyword", "key2"), null, null).get(0).get("value"));
        getDb().update("testtable", "value", "val8", QueryCondition.equals("keyword", "key2"));
        assertEquals("val8", getDb().select("testtable", "value", QueryCondition.equals("keyword", "key2"), null, null).get(0).get("value"));
        getDb().update("testtable", "value", "val2", QueryCondition.equals("keyword", "key2"));
    }

    @Test
    void replace() throws SQLException {
        assertEquals("val2", getDb().select("testtable", "value", QueryCondition.equals("keyword", "key2"), null, null).get(0).get("value"));
        getDb().replace("testtable", new String[] {"keyword", "value"}, new Object[] {"key2", "val10"});
        assertEquals("val10", getDb().select("testtable", "value", QueryCondition.equals("keyword", "key2"), null, null).get(0).get("value"));
        getDb().replace("testtable", new String[] {"keyword", "value"}, new Object[] {"key2", "val2"});
    }

    @Test
    void createTable() throws SQLException {
        assertFalse(getDb().tableExists("temptable"));
        TablePreset.create("temptable").putColumn("var", ColumnType.TEXT.struct()).create(getDb());
        assertTrue(getDb().tableExists("temptable"));
        getDb().drop("temptable");
        assertFalse(getDb().tableExists("temptable"));
    }

    @Test
    void tableExists() throws SQLException {
        assertTrue(getDb().tableExists("testtable"));
        assertFalse(getDb().tableExists("nonexistent"));
    }

    @Test
    void readQuotedString() {
        assertEquals("This is a name", Database.readQuotedString("'This is a name',values={}]"));
    }

    @Test
    void typeConverter() throws SQLException {
        Database.registerTypeConverter(UUID.class, id -> id == null ? null : Database.enquote(id.toString()), UUID::fromString);
        TablePreset.create("typetest").putColumn("id", ColumnType.CHAR.struct().configure(sup -> sup.apply(36))).create(getDb());
        getDb().truncate("typetest");
        UUID id = UUID.randomUUID();
        assertEquals(1, getDb().insert("typetest", "id", id));
        assertEquals(id, getDb().select("typetest", "id").get(0).get("id", UUID.class));
    }

    @Test
    void createIndex() throws SQLException {
        TablePreset.create("indextest").putColumn("col", ColumnType.TEXT.struct()).create(getDb());
        assertDoesNotThrow(() -> getDb().createIndex("indextest", TableIndex.index("fulltexttest", "col", TableIndex.Type.FULLTEXT)));
        getDb().drop("indextest");
    }

    @Test
    void createTableWithIndices() throws SQLException {
        Database db = getDb();
        assertDoesNotThrow(() -> TablePreset.create("indicestest")
                .putColumn("col1", ColumnType.TEXT.struct())
                .putColumn("col2", ColumnType.TEXT.struct())
                .addIndex(TableIndex.index("col1index", "col1", TableIndex.Type.FULLTEXT))
                .addIndex(TableIndex.index("col2index", "col2", TableIndex.Type.INDEX))
                .create(db)); // We're testing if TablePreset#create(Database) throws an error here, not if #getDb() does.
        db.drop("indicestest");
    }

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

    @Test
    void testAsyncExceptionTrace() throws SQLException {
        // Should log the SQLException and the root trace leading to this method
        getDb().selectAsync("nonexistent", "*");
    }

    @Test
    void testSelectJoin() throws SQLException {
        SelectResults res = getDb().selectBuilder("join_test_1")
                .select("*")
                .join(Join.builder()
                        .type(JoinType.INNER)
                        .table("join_test_2")
                        .using("id"))
                .where(QueryCondition.func(new QueryFunction("1"))) // Just to check if this causes any syntax errors
                .execute();

        assertEquals(1, res.size());
        assertEquals("Value from table 1", res.get(0).getString("value1"));
        assertEquals(testId, res.get(0).getUUID("value2"));
    }
}