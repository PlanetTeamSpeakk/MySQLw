package com.ptsmods.mysqlw.query.builder;

import com.ptsmods.mysqlw.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InsertBuilder {
    private final Database db;
    private final String table;
    private final String[] columns;
    private final List<Object[]> values = new ArrayList<>();

    private InsertBuilder(Database db, String table, String[] columns) {
        this.db = db;
        this.table = table;
        this.columns = Arrays.copyOf(columns, columns.length);
    }

    public static InsertBuilder create(Database db, String table, String... columns) {
        return new InsertBuilder(db, table, columns);
    }

    public static InsertBuilder create(Database db, String table, List<String> columns) {
        return new InsertBuilder(db, table, columns.toArray(new String[0]));
    }

    public static InsertBuilder create(String table, String... columns) {
        return new InsertBuilder(null, table, columns);
    }

    public static InsertBuilder create(String table, List<String> columns) {
        return new InsertBuilder(null, table, columns.toArray(new String[0]));
    }

    public InsertBuilder insert(Object... values) {
        if (values.length != columns.length) throw new IllegalArgumentException("Amount of values passed not equal to columns being filled.");

        this.values.add(values);
        return this;
    }

    public InsertBuilder insert(Object[]... values) {
        for (Object[] value : values) insert(value);
        return this;
    }

    public InsertBuilder insert(Iterable<Object[]> values) {
        for (Object[] value : values) insert(value);
        return this;
    }

    public String buildQuery() {
        return buildQuery(new StringBuilder("INSERT"));
    }

    public String buildReplaceQuery() {
        return buildQuery(new StringBuilder("REPLACE"));
    }

    public String buildReplaceUpdateQuery() {
        if (values.size() != 1) throw new IllegalStateException("One set of values must be passed to build a replace update query.");

        StringBuilder query = new StringBuilder("REPLACE INTO ").append(table).append(" SET ");
        for (int i = 0; i < columns.length; i++)
            query.append(columns[i]).append(" = ").append(Database.getAsString(values.get(0)[i])).append(", ");
        query.delete(query.length()-2, query.length()).append(';');

        return query.toString();
    }

    public String buildQuery(StringBuilder query) {
        if (values.isEmpty()) throw new IllegalStateException("No values were specified.");

        query.append(" INTO ").append(Database.engrave(table)).append(" (`").append(String.join("`, `", columns)).append("`) VALUES ");
        for (Object[] valuesArray : values) {
            query.append("(");
            for (Object value : valuesArray)
                query.append(Database.getAsString(value)).append(", ");
            query.delete(query.length()-2, query.length()).append("), ");
        }

        return query.delete(query.length()-2, query.length()).append(';').toString();
    }

    public int execute() {
        return db.executeUpdate(buildQuery());
    }

    public CompletableFuture<Integer> executeAsync() {
        return db.executeUpdateAsync(buildQuery());
    }

    public int executeReplace() {
        return db.executeUpdate(buildReplaceQuery());
    }

    public CompletableFuture<Integer> executeReplaceAsync() {
        return db.executeUpdateAsync(buildReplaceQuery());
    }

    public int executeReplaceUpdate() {
        return db.executeUpdate(buildReplaceUpdateQuery());
    }

    public CompletableFuture<Integer> executeReplaceUpdateAsync() {
        return db.executeUpdateAsync(buildReplaceUpdateQuery());
    }

    @Override
    public InsertBuilder clone() {
        InsertBuilder builder = create(db, table, columns);
        builder.insert(values);
        return builder;
    }
}
