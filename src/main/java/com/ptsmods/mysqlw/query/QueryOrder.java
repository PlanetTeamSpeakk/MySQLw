package com.ptsmods.mysqlw.query;

/**
 * The order in which to return results.
 */
public class QueryOrder {

    public static QueryOrder by(String column) {
        return by(column, OrderDirection.ASC);
    }

    public static QueryOrder by(String column, OrderDirection direction) {
        return new QueryOrder(column, direction);
    }

    private final String column;
    private final OrderDirection direction;

    private QueryOrder(String column, OrderDirection direction) {
        this.column = column;
        this.direction = direction;
    }

    public String getColumn() {
        return column;
    }

    public OrderDirection getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "`" + column + "` " + direction.name();
    }

    public enum OrderDirection {
        ASC, DESC;
    }

}
