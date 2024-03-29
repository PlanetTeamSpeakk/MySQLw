package com.ptsmods.mysqlw.test;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.SilentSQLException;
import com.ptsmods.mysqlw.query.*;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.ForeignKey;
import com.ptsmods.mysqlw.table.TableIndex;
import com.ptsmods.mysqlw.table.TablePreset;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.util.Arrays;
import java.util.Map;
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

    @Test
    void testGroupBy() throws SQLException {
        Database db = getDb();

        db.drop("groupby_test"); // In case it failed last time.
        TablePreset.create("groupby_test")
                .putColumn("id", ColumnType.INT.struct()
                        .setPrimary()
                        .setAutoIncrement()
                        .setNonNull())
                .putColumn("year", ColumnType.YEAR.struct()
                        .setNonNull())
                .putColumn("month", ColumnType.ENUM.struct()
                        .configure(sup -> sup.apply(new String[] {
                                "January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"}))
                        .setNonNull())
                .putColumn("profit", ColumnType.INT.struct()
                        .setNonNull())
                .addIndex(TableIndex.index("year", TableIndex.Type.INDEX))
                .create(db);

        db.insertBuilder("groupby_test", "year", "month", "profit")
                .insert(Year.of(2021), "October", 2150)
                .insert(Year.of(2021), "November", 2100)
                .insert(Year.of(2021), "December", 2250)
                .insert(Year.of(2022), "January", 2300)
                .insert(Year.of(2022), "February", 2450)
                .insert(Year.of(2022), "March", 2200)
                .insert(Year.of(2022), "April", 2550)
                .execute();

        Map<Date, Long> count = db.selectBuilder("groupby_test")
                .select("year", "month", "profit")
                .groupBy("year")
                .executeCountMultiple(Date.class);
        assertEquals(3, count.get(Date.valueOf(LocalDate.of(2021, 1, 1))));
        assertEquals(4, count.get(Date.valueOf(LocalDate.of(2022, 1, 1))));

        SelectResults profit = db.selectBuilder("groupby_test")
                .select("year")
                .select(new QueryFunction("SUM(profit)"), "profit")
                .groupBy("year")
                .execute();

        assertEquals(2150 + 2100 + 2250, profit.get(0).getInt("profit"));
        assertEquals(2300 + 2450 + 2200 + 2550, profit.get(1).getInt("profit"));

        db.drop("groupby_test");
    }

    @Test
    void testSelectBuilderCount() throws SQLException {
        Database db = getDb();

        assertEquals(2, db.selectBuilder("testtable")
                .select("*")
                .executeCount());
    }

    @Test
    void testDeleteLimit() throws SQLException {
        Database db = getDb();

        db.drop("delete_limit_test"); // In case it failed last time.
        TablePreset.create("delete_limit_test")
                .putColumn("col", ColumnType.TEXT.struct())
                .create(db);

        for (int i = 0; i < 3; i++) db.insert("delete_limit_test", "col", 5);

        assertEquals(3, db.count("delete_limit_test", "*"));
        db.delete("delete_limit_test", QueryCondition.equals("col", 5), 1);
        assertEquals(2, db.count("delete_limit_test", "*"));

        db.drop("delete_limit_test");
    }

    @Test
    void testAddModifyDropColumn() throws SQLException {
        Database db = getDb();

        db.drop("column_test"); // In case it failed last time.
        TablePreset.create("column_test")
                .putColumn("test", ColumnType.TEXT.struct())
                .create(db);

        assertDoesNotThrow(() -> db.addColumn("column_test", "test2", ColumnType.INT.struct(), "test"));
        assertDoesNotThrow(() -> db.modifyColumn("column_test", "test2", ColumnType.BIGINT.struct()));
        assertDoesNotThrow(() -> db.dropColumn("column_test", "test2"));

        db.drop("column_test");
    }

    @Test
    void testForeignKeys() throws SQLException {
        Database db = getDb();

        // Child must be dropped before parent as MySQL does not allow otherwise due to foreign keys.
        db.drop("foreign_key_test_child"); // In case it failed last time.
        db.drop("foreign_key_test_parent"); // In case it failed last time.
        TablePreset.create("foreign_key_test_parent")
                .putColumn("id", ColumnType.INT.struct()
                        .setPrimary()
                        .setAutoIncrement()
                        .setNonNull())
                .putColumn("foo", ColumnType.TEXT.struct())
                .create(db);

        TablePreset.create("foreign_key_test_child")
                .putColumn("parent", ColumnType.INT.struct()
                        .setNonNull())
                .putColumn("foo", ColumnType.TEXT.struct())
                .addForeignKey(ForeignKey.builder()
                        .column("parent")
                        .referenceTable("foreign_key_test_parent")
                        .referenceColumn("id")
                        .onDelete(ForeignKey.Action.CASCADE)
                        .onUpdate(ForeignKey.Action.CASCADE))
                .create(db);

        db.insertBuilder("foreign_key_test_parent", "id", "foo")
                .insert(1, "bar")
                .insert(2, "baz")
                .execute();

        db.insertBuilder("foreign_key_test_child", "parent", "foo")
                .insert(1, "foobar")
                .insert(1, "barfoo")
                .insert(1, "barbaz")
                .insert(2, "barfoo")
                .insert(2, "barbaz")
                .execute();

        assertEquals(3, db.count("foreign_key_test_child", "*", QueryCondition.equals("parent", 1)));
        assertEquals(2, db.count("foreign_key_test_child", "*", QueryCondition.equals("parent", 2)));

        // Update the id of the parent with id 1,
        db.update("foreign_key_test_parent", "id", 3, QueryCondition.equals("id", 1));
        assertEquals(3, db.count("foreign_key_test_child", "*", QueryCondition.equals("parent", 3)));

        // Delete the parent with id 2, should also remove all children rows with parent id 2
        db.delete("foreign_key_test_parent", QueryCondition.equals("id", 2));
        assertEquals(0, db.count("foreign_key_test_child", "*", QueryCondition.equals("parent", 2)));

        db.drop("foreign_key_test_child");
        db.drop("foreign_key_test_parent");
    }

    @Test
    void testChecks() throws SQLException {
        Database db = getDb();

        db.drop("checks_test");
        TablePreset.create("checks_test")
                .putColumn("value", ColumnType.INT.struct())
                .addCheck(QueryCondition.lessEqual("value", 10))
                .create(db);

        assertDoesNotThrow(() -> db.insert("checks_test", "value", 5));
        assertDoesNotThrow(() -> db.insert("checks_test", "value", 10));
        assertThrows(SilentSQLException.class, () -> db.insert("checks_test", "value", 15));

        db.drop("checks_test");
    }
}