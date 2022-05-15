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
	private QueryCondition condition;
	private QueryOrder order;
	private QueryLimit limit;

	private SelectBuilder(Database db, String table) {
		this.db = db;
		this.table = table;
	}

	public static SelectBuilder create(Database db, String table) {
		return new SelectBuilder(db, table);
	}

	public SelectBuilder select(CharSequence column) {
		return select(column, null);
	}

	public SelectBuilder select(CharSequence column, String name) {
		columns.add(new Pair<>(column, name));
		return this;
	}

	public SelectBuilder select(CharSequence... columns) {
		for (CharSequence column : columns) select(column);
		return this;
	}

	public SelectBuilder select(Iterable<CharSequence> columns) {
		for (CharSequence column : columns) select(column);
		return this;
	}

	public SelectBuilder where(QueryCondition condition) {
		this.condition = condition;
		return this;
	}

	public SelectBuilder order(QueryOrder order) {
		this.order = order;
		return this;
	}

	public SelectBuilder order(String column) {
		return order(QueryOrder.by(column));
	}

	public SelectBuilder order(String column, QueryOrder.OrderDirection direction) {
		return order(QueryOrder.by(column, direction));
	}

	public SelectBuilder limit(QueryLimit limit) {
		this.limit = limit;
		return this;
	}

	public SelectBuilder limit(int limit) {
		return limit(QueryLimit.limit(limit));
	}

	public SelectBuilder limit(int limit, int offset) {
		return limit(QueryLimit.limit(limit, offset));
	}

	public String buildQuery() {
		StringBuilder query = new StringBuilder("SELECT ");
		for (Pair<CharSequence, String> seq : columns)
			query.append(Database.getAsString(seq.getLeft())).append(seq.getRight() == null ? "" : " AS " + Database.engrave(seq.getRight())).append(", ");

		query.delete(query.length()-2, query.length())
				.append(" FROM ").append(Database.engrave(table))
				.append(condition == null ? "" : " WHERE " + condition)
				.append(order == null ? "" : " ORDER BY " + order)
				.append(limit == null ? "" : " " + limit);

		return query.toString();
	}

	public ResultSet executeRaw() {
		return db.executeQuery(buildQuery());
	}

	public CompletableFuture<ResultSet> executeRawAsync() {
		return db.executeQueryAsync(buildQuery());
	}

	public SelectResults execute() {
		return SelectResults.parse(db, table, executeRaw(), condition, order, limit);
	}

	public CompletableFuture<SelectResults> executeAsync() {
		return db.runAsync(this::execute);
	}
}
