package com.ptsmods.mysqlw.table;

import com.google.common.collect.ImmutableList;
import com.ptsmods.mysqlw.Database;
import com.google.common.collect.ImmutableMap;
import javafx.util.Pair;

import java.util.*;

public class TablePreset {

    public static TablePreset create(String name) {
        return new TablePreset(name);
    }

    private final Map<String, ColumnStructure<?>> columns = new LinkedHashMap<>();
    private final Map<TableIndex.TableIndexType, TableIndex> indices = new HashMap<>();
    private String name;

    private TablePreset(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TablePreset setName(String name) {
        this.name = name;
        return this;
    }

    public TablePreset putAll(Map<String, ColumnStructure<?>> columns) {
        this.columns.putAll(columns);
        return this;
    }

    public TablePreset putColumn(String name, ColumnStructure<?> structure) {
        columns.put(name, structure);
        return this;
    }

    public TablePreset removeColumn(String name) {
        columns.remove(name);
        return this;
    }

    public TablePreset addIndex(TableIndex index) {
        indices.put(index.getType(), index);
        return this;
    }

    public TablePreset removeIndex(TableIndex index) {
        indices.remove(index.getType());
        return this;
    }

    public Map<String, ColumnStructure<?>> getColumns() {
        return ImmutableMap.copyOf(columns);
    }

    public Map<String, String> build(Database.RDBMS type) {
        return ImmutableMap.copyOf(Database.convertMap(columns, entry -> new Pair<>(entry.getKey(), entry.getValue().toString(type))));
    }

    public List<TableIndex> getIndices() {
        return ImmutableList.copyOf(indices.values());
    }

    public TableIndex getIndex(TableIndex.TableIndexType type) {
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
