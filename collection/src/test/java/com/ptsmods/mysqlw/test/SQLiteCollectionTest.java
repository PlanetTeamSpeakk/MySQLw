package com.ptsmods.mysqlw.test;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.collection.DbList;
import com.ptsmods.mysqlw.collection.DbMap;
import com.ptsmods.mysqlw.collection.DbSet;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ALL")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SQLiteCollectionTest {
    private static Database db = null;

    Database getDb() throws SQLException {
        if (db == null) {
            db = Database.connect(new File("sqlite.db"));
            db.setLogging(false);
        }

        return db;
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
}