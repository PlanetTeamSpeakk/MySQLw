package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of {@link QueryCondition}s which a query must meet to affect or return rows.
 */
public class QueryConditions extends QueryCondition {

    /**
     * @return Empty QueryConditions.
     */
    public static QueryConditions create() {
        return create(null);
    }

    /**
     * @param first The first {@link QueryCondition} in these QueryConditions.
     * @return QueryConditions with the given QueryCondition.
     */
    public static QueryConditions create(QueryCondition first) {
        return new QueryConditions(first);
    }

    private final List<Pair<ConditionKeyword, QueryCondition>> conditions = new ArrayList<>();

    private QueryConditions(QueryCondition first) {
        if (first != null) add(null, first); // The keyword of the first argument doesn't matter.
    }

    /**
     * Adds a QueryCondition with the {@link ConditionKeyword#AND AND} keyword to the end.
     * @param condition The actual condition
     * @return These QueryConditions for chaining.
     */
    public QueryConditions and(QueryCondition condition) {
        return add(ConditionKeyword.AND, condition);
    }

    /**
     * Adds a QueryCondition with the {@link ConditionKeyword#AND AND} keyword at a specific index.
     * @param index The index to add this QueryCondition at
     * @param condition The actual condition
     * @return These QueryConditions for chaining.
     */
    public QueryConditions and(int index, QueryCondition condition) {
        return add(index, ConditionKeyword.AND, condition);
    }

    /**
     * Adds a QueryCondition with the {@link ConditionKeyword#OR OR} keyword to the end.
     * @param condition The actual condition
     * @return These QueryConditions for chaining.
     */
    public QueryConditions or(QueryCondition condition) {
        return add(ConditionKeyword.OR, condition);
    }

    /**
     * Adds a QueryCondition with the {@link ConditionKeyword#OR OR} keyword at a specific index.
     * @param index The index to add this QueryCondition at
     * @param condition The actual condition
     * @return These QueryConditions for chaining.
     */
    public QueryConditions or(int index, QueryCondition condition) {
        return add(index, ConditionKeyword.OR, condition);
    }

    /**
     * Adds a QueryCondition with a specific keyword to the end.
     * @param keyword The keyword this QueryCondition uses
     * @param condition The actual condition
     * @return These QueryConditions for chaining.
     */
    public QueryConditions add(ConditionKeyword keyword, QueryCondition condition) {
        conditions.add(new Pair<>(keyword, condition));
        return this;
    }

    /**
     * Adds a new QueryCondition at a specific index with a specific keyword.
     * @param index The index to add this QueryCondition at
     * @param keyword The keyword this QueryCondition uses
     * @param condition The actual condition
     * @return These QueryConditions for chaining.
     */
    public QueryConditions add(int index, ConditionKeyword keyword, QueryCondition condition) {
        conditions.add(index, new Pair<>(keyword, condition));
        return this;
    }

    @Override
    public String toString() {
		// No need to use parentheses when there's only one condition.
		if (conditions.size() == 1) return conditions.get(0).toString();

        StringBuilder builder = new StringBuilder("(");
        boolean first = true;
        for (Pair<ConditionKeyword, QueryCondition> condition : conditions) {
            builder.append(first ? "" : condition.getLeft().toString() + " ").append(condition.getRight()).append(" ");
            first = false;
        }
        return builder.toString().trim() + ')';
    }

    public enum ConditionKeyword {
        /**
         * Implies that both the previous QueryConditions and the next one must be true to return a row.
         */
        AND,
        /**
         * Implies that either the previous QueryConditions or the next one must be true to return a row.
         */
        OR
    }

}
