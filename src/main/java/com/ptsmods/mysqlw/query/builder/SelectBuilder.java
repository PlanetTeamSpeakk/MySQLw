package com.ptsmods.mysqlw.query.builder;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.Pair;
import com.ptsmods.mysqlw.query.*;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SelectBuilder {
    private final Database db;
    private final String selectionTarget;
    private final List<Pair<CharSequence, String>> columns = new ArrayList<>();
    private String alias;
    private final Set<Join> joins = new LinkedHashSet<>();
    private String target;
    private QueryCondition condition;
    private GroupBy groupBy;
    private QueryOrder order;
    private QueryLimit limit;

    private SelectBuilder(Database db, String selectionTarget) {
        this.db = db;
        this.selectionTarget = selectionTarget;
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
     * Create a new SelectBuilder selecting data from another select query on the given {@link Database}.
     * @param db The database to select from
     * @param select The query to select from
     * @return A new SelectBuilder
     */
    public static SelectBuilder create(Database db, SelectBuilder select) {
        return new SelectBuilder(db, "(" + select.buildQuery() + ")");
    }

    /**
     * Create a new SelectBuilder for use in {@link com.ptsmods.mysqlw.procedure.IBlockBuilder BlockBuilder}s.
     * @param table The table to select from
     * @return A new SelectBuilder
     */
    public static SelectBuilder create(String table) {
        return new SelectBuilder(null, table);
    }

    /**
     * Create a new SelectBuilder for use in {@link com.ptsmods.mysqlw.procedure.IBlockBuilder BlockBuilder}s.
     * @param select The query to select from
     * @return A new SelectBuilder
     */
    public static SelectBuilder create(SelectBuilder select) {
        return new SelectBuilder(null, "(" + select.buildQuery() + ")");
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
    public SelectBuilder select(Iterable<? extends CharSequence> columns) {
        for (CharSequence column : columns) select(column);
        return this;
    }

    /**
     * Sets the alias of the table to select from used when selecting.<br>
     * Mostly only useful in combination with {@link #join(Join)}
     * @param alias The alias to use
     * @return This SelectBuilder
     */
    public SelectBuilder alias(String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * Add a new {@link JoinType#INNER INNER} join.
     * @param table The table to join with
     * @param condition The condition to join on, required unless chaining multiple joins on MySQL.
     * @return This select builder
     */
    public SelectBuilder join(String table, QueryCondition condition) {
        return join(JoinType.INNER, table, condition);
    }

    /**
     * Add a new join.
     * @param type The type of the new join
     * @param table The table to join with
     * @param condition The condition to join on, required unless chaining multiple joins on MySQL.
     * @return This SelectBuilder
     */
    public SelectBuilder join(JoinType type, String table, QueryCondition condition) {
        return join(Join.builder()
                .type(type)
                .table(table)
                .condition(condition));
    }

    /**
     * Add a new join formed with a builder.
     * @param join The join to add
     * @return This SelectBuilder
     */
    public SelectBuilder join(Join.Builder join) {
        return join(join.build());
    }

    /**
     * Add a new prebuilt join.
     * @param join The join to add
     * @return This SelectBuilder
     */
    public SelectBuilder join(Join join) {
        joins.add(join);
        return this;
    }

    public SelectBuilder removeJoin(Join join) {
        joins.remove(join);
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
     * Groups this query by the specified columns, especially useful in combination with aggregate functions.
     * @param columns The columns to group by
     * @return This SelectBuilder
     */
    public SelectBuilder groupBy(String... columns) {
        return groupBy(GroupBy.builder()
                .columns(Arrays.stream(columns).collect(Collectors.toList())));
    }

    /**
     * Groups this query by the given group by.
     * @param groupBy The group by to group by
     * @return This Select Builder
     */
    public SelectBuilder groupBy(GroupBy.Builder groupBy) {
        return groupBy(groupBy.build());
    }

    /**
     * Groups this query by the given group by.
     * @param groupBy The group by to group by
     * @return This Select Builder
     */
    public SelectBuilder groupBy(GroupBy groupBy) {
        this.groupBy = groupBy;
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

        if (columns.isEmpty())
            throw new IllegalStateException("Cannot build a query without having any columns to select.");

        for (Pair<CharSequence, String> seq : columns)
            query.append(Database.getAsString(seq.getLeft())).append(seq.getRight() == null ? "" : " AS " + Database.engrave(seq.getRight())).append(", ");

        query.delete(query.length()-2, query.length())
                .append(target == null ? "" : "INTO " + target)
                .append(selectionTarget == null ? "" : " FROM " + Database.engrave(selectionTarget))
                .append(alias == null ? "" : " AS " + Database.engrave(alias))
                .append(joins.isEmpty() ? "" : " " + joins.stream()
                        .map(Join::toString)
                        .collect(Collectors.joining(" ")))
                .append(condition == null ? "" : " WHERE " + condition)
                .append(groupBy == null ? "" : " " + groupBy.toString(db == null ? Database.RDBMS.UNKNOWN : db.getType()))
                .append(order == null ? "" : " ORDER BY " + order)
                .append(limit == null ? "" : " " + limit);

        return query.toString();
    }

    /**
     * Executes the built query and returns the raw {@link ResultSet}.
     * @return The raw {@link ResultSet}
     */
    public ResultSet executeRaw() {
        if (db == null) throw new IllegalStateException("Cannot execute a query built for use in BlockBuilders.");
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
        return SelectResults.parse(db, selectionTarget, executeRaw(), condition, order, limit);
    }

    /**
     * Asynchronously executes the built query and parses it into an instance of {@link SelectResults}.
     * @return A {@link CompletableFuture} contain the parsed results
     */
    public CompletableFuture<SelectResults> executeAsync() {
        return db.runAsync(this::execute);
    }

    /**
     * Counts the rows this select builder will select.
     * @return The amount of rows counted
     */
    public long executeCount() {
        return executeCountRaw().get(0).getLong("count");
    }

    /**
     * Counts the rows this select builder will select asynchronously.
     * @return A {@link CompletableFuture} containing the counted rows
     */
    public CompletableFuture<Long> executeCountAsync() {
        return db.runAsync(this::executeCount);
    }

    /**
     * Counts the rows this select builder will select.
     * @param type The type of the column this query will be grouped by.
     * @return The amount of rows counted
     */
    public <T> Map<T, Long> executeCountMultiple(Class<T> type) {
        if (groupBy == null || groupBy.getColumns().size() != 1)
            throw new IllegalStateException("Count Multiple can only be used on a SelectBuilder that groups by one column.");

        return executeCountRaw().stream()
                .collect(Collectors.toMap(row -> row.get(groupBy.getColumns().get(0), type), row -> row.getLong("count")));
    }

    /**
     * Counts the rows this select builder will select asynchronously.
     * @param type The type of the column this query will be grouped by.
     * @return A {@link CompletableFuture} containing the counted rows
     */
    public <T> CompletableFuture<Map<T, Long>> executeCountMultipleAsync(Class<T> type) {
        return db.runAsync(() -> executeCountMultiple(type));
    }

    public SelectResults executeCountRaw() {
        ArrayList<Pair<CharSequence, String>> columnsCopy = new ArrayList<>(columns);
        columns.clear();
        if (groupBy != null)
            groupBy.getColumns().forEach(column -> columns.add(new Pair<>(column, null)));

        columns.add(new Pair<>(new QueryFunction("COUNT(" + (columnsCopy.size() != 1 ? "*" : Database.getAsString(columnsCopy.get(0).getLeft())) + ")"), "count"));
        return execute();
    }

    public CompletableFuture<SelectResults> executeCountRawAsync() {
        return db.runAsync(this::executeCountRaw);
    }

    /**
     * @return The database this builder will select from
     */
    public Database getDb() {
        return db;
    }

    /**
     * @return The table this builder will select from
     * @deprecated Has been renamed to account for different usages. Use {@link #getSelectionTarget()} instead.
     */
    @Deprecated
    public String getTable() {
        return selectionTarget;
    }

    /**
     * @return The table or query this builder will select from
     */
    public String getSelectionTarget() {
        return selectionTarget;
    }

    /**
     * @return The columns this builder will select, mainly for internal purposes.
     */
    public List<Pair<CharSequence, String>> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * @return The alias of this table used when selecting
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return The target this builder will select into
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return The condition rows will have to comply with to be selected
     */
    public QueryCondition getCondition() {
        return condition;
    }

    /**
     * @return The maximum amount of rows this builder will return upon selecting
     */
    public QueryLimit getLimit() {
        return limit;
    }

    /**
     * @return The order in which the rows will be sorted upon selecting
     */
    public QueryOrder getOrder() {
        return order;
    }

    /**
     * @return The joins added to this builder
     */
    public Set<Join> getJoins() {
        return Collections.unmodifiableSet(joins);
    }

    /**
     * @return The group by statement of this builder
     */
    public GroupBy getGroupBy() {
        return groupBy;
    }

    @Override
    public SelectBuilder clone() {
        SelectBuilder builder = create(db, selectionTarget);
        for (Pair<CharSequence, String> column : columns) builder.select(column.getLeft(), column.getRight());
        builder.into(target);
        builder.where(condition);
        builder.limit(limit);
        builder.order(order);

        return builder;
    }
}
