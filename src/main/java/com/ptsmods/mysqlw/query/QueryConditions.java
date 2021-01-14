package com.ptsmods.mysqlw.query;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of {@link QueryCondition}s which a query must meet to affect or return rows.
 */
public class QueryConditions extends QueryCondition {

    public static QueryConditions create() {
        return create(null);
    }

    public static QueryConditions create(QueryCondition first) {
        return new QueryConditions(first);
    }

    private QueryConditions(QueryCondition first) {
        if (first != null) add(null, first); // The keyword of the first argument doesn't matter.
    }

    private final List<Pair<ConditionKeyword, QueryCondition>> conditions = new ArrayList<>();

    public QueryConditions and(QueryCondition condition) {
        return add(ConditionKeyword.AND, condition);
    }

    public QueryConditions and(int index, QueryCondition condition) {
        return add(index, ConditionKeyword.AND, condition);
    }

    public QueryConditions or(QueryCondition condition) {
        return add(ConditionKeyword.OR, condition);
    }

    public QueryConditions or(int index, QueryCondition condition) {
        return add(index, ConditionKeyword.OR, condition);
    }

    public QueryConditions add(ConditionKeyword keyword, QueryCondition condition) {
        conditions.add(new Pair<>(keyword, condition));
        return this;
    }

    public QueryConditions add(int index, ConditionKeyword keyword, QueryCondition condition) {
        conditions.add(index, new Pair<>(keyword, condition));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        boolean first = true;
        for (Pair<ConditionKeyword, QueryCondition> condition : conditions) {
            builder.append(first ? "" : condition.getKey().toString() + " ").append(condition.getValue()).append(" ");
            first = false;
        }
        return builder.append(')').toString();
    }

    public enum ConditionKeyword {
        AND, OR;
    }

}
