package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;

/**
 * The order in which to return results.
 */
public class QueryOrder {

    /**
     * Order by a column in an {@link OrderDirection#ASC ascending direction}.
     * @param column The column to order by.
     * @return A QueryOrder ordering the given column in an ascending direction.
     */
    public static QueryOrder by(String column) {
        return by(column, OrderDirection.ASC);
    }

    /**
     * Order by a column in the given direction.
     * @param column The column to order by.
     * @param direction The direction to order in.
     * @return A QueryOrder ordering the given column in the given direction.
     */
    public static QueryOrder by(String column, OrderDirection direction) {
        return new QueryOrder(column, direction);
    }

    private final String column;
    private final OrderDirection direction;

    private QueryOrder(String column, OrderDirection direction) {
        this.column = column;
        this.direction = direction;
    }

    /**
     * @return The column this QueryOrder orders by.
     */
    public String getColumn() {
        return column;
    }

    /**
     * @return The direction in which this QueryOrder orders.
     */
    public OrderDirection getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return Database.engrave(column) + " " + direction.name();
    }

    public enum OrderDirection {
        /**
         * Ascending order (e.g. 1 -> 2 -> 3 -> 4)
         */
        ASC,
        /**
         * Descending order (e.g 4 -> 3 -> 2 -> 1)
         */
        DESC
    }

}
