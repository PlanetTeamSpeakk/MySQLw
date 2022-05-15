package com.ptsmods.mysqlw;

import java.util.Objects;

public class Pair<L, R> {
    private final L left;
    private final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pair)) return false;
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(getLeft(), pair.getLeft()) && Objects.equals(getRight(), pair.getRight());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getLeft(), getRight());
	}
}
