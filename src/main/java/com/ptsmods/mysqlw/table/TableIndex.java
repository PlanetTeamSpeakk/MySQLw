package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Table indices, used to speed up queries.
 */
public class TableIndex {

    public static TableIndex index(String name, String column, Type type) {
        return new TableIndex(name, column, type);
    }

    public static TableIndex index(String column, Type type) {
        return new TableIndex(null, column, type);
    }

    public static TableIndex composite(String... columns) {
        return composite(null, columns);
    }

    public static TableIndex composite(String name, String... columns) {
        return new TableIndex(name, Arrays.stream(columns).<LinkedHashSet<String>>collect(LinkedHashSet::new, Set::add, Set::addAll), Type.INDEX);
    }

    private final String name;
    private final Set<String> columns;
    private final Type type;

    private TableIndex(String name, @NonNull String column, @NonNull Type type) {
        this(name, Collections.singleton(column), type);
    }

    private TableIndex(String name, @NonNull Set<String> columns, @NonNull Type type) {
        if (columns.isEmpty()) throw new IllegalArgumentException("Cannot create an index on 0 columns.");
        if (columns.size() > 16) throw new IllegalArgumentException("Composite indices may not spread over more than 16 columns.");

        this.name = name;
        this.columns = Collections.unmodifiableSet(columns);
        this.type = type;
    }

    /**
     * @return The name of this index, may be null.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return Either the first column of this composite index or the only column if this index isn't composite.
     * @see #isComposite()
     */
    public String getColumn() {
        return columns.iterator().next();
    }

    /**
     * @return All columns of this index. Will contain a single element if this index isn't composite, otherwise up to 16.
     */
    public Set<String> getColumns() {
        return columns;
    }

    public Type getType() {
        return type;
    }

    public boolean isComposite() {
        return columns.size() > 1;
    }

    @Override
    public TableIndex clone() {
        return new TableIndex(name, columns, type);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeColumn) {
        return type.toString(name, columns, includeColumn);
    }

    public enum Type {
        /**
         * Each row must have a unique value for this column.
         */
        UNIQUE,
        /**
         * Used to speed up queries.
         */
        INDEX,
        /**
         * Allows you to match pieces of text against the value of this column.
         * To match case-sensitively, have a look at {@link ColumnAttributes#BINARY BINARY}.
         */
        FULLTEXT,
        /**
         * Indices for {@link ColumnType#GEOMETRY GEOMETRY} objects.
         */
        SPATIAL;

        public String toString(String name, Set<String> columns, boolean includeColumn) {
            return (this == INDEX ? "" : name() + " ") + "INDEX " + (name == null ? "" : Database.engrave(name) + " ") +
                    (includeColumn ? "(" + columns.stream()
                            .map(Database::engrave)
                            .collect(Collectors.joining(", ")) + ")" : "");
        }
    }
}
