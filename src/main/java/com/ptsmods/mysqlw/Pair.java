package com.ptsmods.mysqlw;

import lombok.Data;

@Data
public class Pair<L, R> {
    private static final Pair<Object, Object> EMPTY = new Pair<>(null, null);
    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings("unchecked") // It's an empty immutable object
    public static <L, R> Pair<L, R> empty() {
        return (Pair<L, R>) EMPTY;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
