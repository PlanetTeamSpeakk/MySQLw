package com.ptsmods.mysqlw.query;

/**
 * Indicates a limit of the amount of rows you wish to have returned.
 * Can optionally also indicate an offset.
 */
public class QueryLimit {
    public static QueryLimit limit(int limit) {
        return new QueryLimit(limit, -1);
    }

    public static QueryLimit limit(int limit, int offset) {
        return new QueryLimit(limit, offset);
    }

    private final int limit, offset;

    private QueryLimit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "LIMIT " + limit + (offset >= 0 ? " OFFSET " + offset : "");
    }
}
