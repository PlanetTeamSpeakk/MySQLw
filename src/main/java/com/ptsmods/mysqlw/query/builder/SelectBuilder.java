package com.ptsmods.mysqlw.query.builder;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.Pair;
import com.ptsmods.mysqlw.query.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SelectBuilder {
    private final Database db;
    private final String table;
    private final List<Pair<CharSequence, String>> columns = new ArrayList<>();
    private String target;
    private QueryCondition condition;
    private QueryOrder order;
    private QueryLimit limit;

    private SelectBuilder(Database db, String table) {
        this.db = db;
        this.table = table;
    }

    /**
     * Create a new SelectBuilder selecting data from the given table on the given {@link Database}.
     * @param db The database to select from
     * @param table The table to select from
     * @return A new SelectBuilder
     */
    public static SelectBuilder create(Database db, String table) {
        return new SelectBuilder(db, table);
    }

    /**
     * Select a column or {@link QueryFunction}.
     * @param column The column or {@link QueryFunction} to select
     * @return This SelectBuilder
     */
    public SelectBuilder select(CharSequence column) {
        return select(column, null);
    }

    /**
     * Select a column or {@link QueryFunction} with a specific name.
     * @param column The column or {@link QueryFunction} to select
     * @param name The name to select the column as
     * @return This SelectBuilder
     */
    public SelectBuilder select(CharSequence column, String name) {
        columns.add(new Pair<>(column, name));
        return this;
    }

    /**
     * Select columns or {@link QueryFunction}s or any combination.
     * @param columns The columns or {@link QueryFunction}s or any combination to select
     * @return This SelectBuilder
     */
    public SelectBuilder select(CharSequence... columns) {
        for (CharSequence column : columns) select(column);
        return this;
    }

    /**
     * Select columns or {@link QueryFunction}s or any combination.
     * @param columns The columns or {@link QueryFunction}s or any combination to select
     * @return This SelectBuilder
     */
    public SelectBuilder select(Iterable<CharSequence> columns) {
        for (CharSequence column : columns) select(column);
        return this;
    }

    /**
     * Sets the table or variable to store the selected data into.
     * @param target The target to select into
     * @return This SelectBuilder
     */
    public SelectBuilder into(String target) {
        this.target = target;
        return this;
    }

    /**
     * Sets the condition that rows must comply with to be selected.
     * @param condition The condition that rows must comply with
     * @return This SelectBuilder
     */
    public SelectBuilder where(QueryCondition condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Sets the order in which the data should be returned.
     * @param order The order in which the data should be returned
     * @return This SelectBuilder
     */
    public SelectBuilder order(QueryOrder order) {
        this.order = order;
        return this;
    }

    /**
     * Sets the order in which the data should be returned.<br>
     * Convenient shorthand for {@link #order(QueryOrder)} and {@link QueryOrder#by(String)}.
     * @param column The order in which the data should be returned
     * @return This SelectBuilder
     */
    public SelectBuilder order(String column) {
        return order(QueryOrder.by(column));
    }

    /**
     * Sets the order in which the data should be returned.<br>
     * Convenient shorthand for {@link #order(QueryOrder)} and {@link QueryOrder#by(String, QueryOrder.OrderDirection)}.
     * @param column The order in which the data should be returned
     * @param direction The direction to order in
     * @return This SelectBuilder
     */
    public SelectBuilder order(String column, QueryOrder.OrderDirection direction) {
        return order(QueryOrder.by(column, direction));
    }

    /**
     * Set the maximum amount of rows that should be returned.
     * @param limit The maximum amount of rows to return
     * @return This SelectBuilder
     */
    public SelectBuilder limit(QueryLimit limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set the maximum amount of rows that should be returned.<br>
     * Convenience method for {@link #limit(QueryLimit)} and {@link QueryLimit#limit(int)}.
     * @param limit The maximum amount of rows to return
     * @return This SelectBuilder
     */
    public SelectBuilder limit(int limit) {
        return limit(QueryLimit.limit(limit));
    }

    /**
     * Set the maximum amount of rows that should be returned.<br>
     * Convenience method for {@link #limit(QueryLimit)} and {@link QueryLimit#limit(int, int)}.
     * @param limit The maximum amount of rows to return
     * @param offset The offset at which to start
     * @return This SelectBuilder
     */
    public SelectBuilder limit(int limit, int offset) {
        return limit(QueryLimit.limit(limit, offset));
    }

    /**
     * Builds a {@code SELECT} query from this builder.
     * @return The built query
     */
    public String buildQuery() {
        StringBuilder query = new StringBuilder("SELECT ");
        for (Pair<CharSequence, String> seq : columns)
            query.append(Database.getAsString(seq.getLeft())).append(seq.getRight() == null ? "" : " AS " + Database.engrave(seq.getRight())).append(", ");

        query.delete(query.length()-2, query.length())
                .append(target == null ? "" : "INTO " + target)
                .append(" FROM ").append(Database.engrave(table))
                .append(condition == null ? "" : " WHERE " + condition)
                .append(order == null ? "" : " ORDER BY " + order)
                .append(limit == null ? "" : " " + limit);

        return query.toString();
    }

    /**
     * Executes the built query and returns the raw {@link ResultSet}.
     * @return The raw {@link ResultSet}
     */
    public ResultSet executeRaw() {
        return db.executeQuery(buildQuery());
    }

    /**
     * {@link #executeRaw()} but asynchronous.
     * @return A {@link CompletableFuture} containing the raw {@link ResultSet}
     */
    public CompletableFuture<ResultSet> executeRawAsync() {
        return db.executeQueryAsync(buildQuery());
    }

    /**
     * Executes the built query and parses it into an instance of {@link SelectResults}.
     * @return The parsed results
     */
    public SelectResults execute() {
        return SelectResults.parse(db, table, executeRaw(), condition, order, limit);
    }

    /**
     * Asynchronously executes the built query and parses it into an instance of {@link SelectResults}.
     * @return A {@link CompletableFuture} contain the parsed results
     */
    public CompletableFuture<SelectResults> executeAsync() {
        return db.runAsync(this::execute);
    }
}
