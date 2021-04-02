package com.ptsmods.mysqlw.query;

/**
 * Indicates a limit of the amount of rows you wish to have returned.
 * Can optionally also indicate an offset.
 */
public class QueryLimit {
    /**
     * @param limit The maximum amount of rows a query may return
     * @return A QueryLimit that limits the amount of rows returned by a query
     */
    public static QueryLimit limit(int limit) {
        return limit(limit, -1);
    }

    /**
     * @param limit The maximum amount of rows a query may return
     * @param offset The offset at which the RDBMS should get these rows
     * @return A QueryLimit that limits the amount of rows returned by a query and gets them at a specific offset.
     */
    public static QueryLimit limit(int limit, int offset) {
        return new QueryLimit(limit, offset);
    }

    private final int limit, offset;

    private QueryLimit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }

    /**
     * @return The maximum amount of rows this QueryLimit allows a query to return.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return The offset at which the RDBMS should get the rows.
     */
    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "LIMIT " + limit + (offset >= 0 ? " OFFSET " + offset : "");
    }
}
