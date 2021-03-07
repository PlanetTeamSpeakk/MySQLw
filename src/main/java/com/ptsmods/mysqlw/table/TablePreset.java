package com.ptsmods.mysqlw.table;

import com.google.common.collect.ImmutableList;
import com.ptsmods.mysqlw.Database;
import com.google.common.collect.ImmutableMap;
import com.ptsmods.mysqlw.Pair;

import java.util.*;

public class TablePreset {

    /**
     * Create a new TablePreset with the given name.
     * @param name The name of the new TablePreset.
     * @return A new TablePreset with the given name.
     */
    public static TablePreset create(String name) {
        return new TablePreset(name);
    }

    private final Map<String, ColumnStructure<?>> columns = new LinkedHashMap<>();
    private final Map<TableIndexType.Type, TableIndexType> indices = new HashMap<>();
    private String name;

    private TablePreset(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Changes the name of this TablePreset.
     * @param name The new name
     * @return This TablePreset
     */
    public TablePreset setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Puts all columns with their names into this TablePreset.
     * @param columns The columns to add.
     * @return This TablePreset
     */
    public TablePreset putAll(Map<String, ColumnStructure<?>> columns) {
        this.columns.putAll(columns);
        return this;
    }

    /**
     * Puts a new column into this TablePreset.
     * @param name The name of the new column
     * @param structure The {@link ColumnStructure} of the column
     * @return This TablePreset
     */
    public TablePreset putColumn(String name, ColumnStructure<?> structure) {
        columns.put(name, structure);
        return this;
    }

    /**
     * Removes a column by its name.
     * @param name The name of the column to remove.
     * @return This TablePreset
     */
    public TablePreset removeColumn(String name) {
        columns.remove(name);
        return this;
    }

    /**
     * Adds a {@link TableIndexType} to this preset.
     * @param index The {@link TableIndexType} to add
     * @return This TablePreset
     */
    public TablePreset addIndex(TableIndexType index) {
        indices.put(index.getType(), index);
        return this;
    }

    /**
     * Removes an index by its type.
     * @param type The type of the index to remove.
     * @return This TablePreset
     */
    public TablePreset removeIndex(TableIndexType type) {
        indices.remove(type.getType());
        return this;
    }

    /**
     * @return An immutable copy of the map of columns in this
     */
    public Map<String, ColumnStructure<?>> getColumns() {
        return ImmutableMap.copyOf(columns);
    }

    public Map<String, String> build(Database.RDBMS type) {
        return ImmutableMap.copyOf(Database.convertMap(columns, entry -> new Pair<>(entry.getKey(), entry.getValue().toString(type))));
    }

    public String buildQuery(Database.RDBMS type) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + getName() + " (");
        build(type).forEach((key, value) -> query.append(key).append(' ').append(value).append(", "));
        if (type != Database.RDBMS.SQLite) getIndices().forEach(index -> query.append(index).append(", ")); // Indices do not work on SQLite apparently.
        if (getColumns().size() > 0) query.delete(query.length() - 2, query.length());
        query.append(");");
        return query.toString();
    }

    public List<TableIndexType> getIndices() {
        return ImmutableList.copyOf(indices.values());
    }

    public TableIndexType getIndex(TableIndexType.Type type) {
        return indices.get(type);
    }

    public TablePreset create(Database db) {
        db.createTable(this);
        return this;
    }

    public TablePreset clone() {
        TablePreset clone = new TablePreset(name);
        clone.putAll(Database.convertMap(columns, entry -> new Pair<>(entry.getKey(), entry.getValue().clone())));
        clone.indices.putAll(Database.convertMap(indices, entry -> new Pair<>(entry.getKey(), entry.getValue().clone())));
        return clone;
    }

}
