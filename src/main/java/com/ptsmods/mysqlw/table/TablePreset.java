package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryCondition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private final List<TableIndex> indices = new ArrayList<>();
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private final List<QueryCondition> checks = new ArrayList<>();
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
     * Adds a {@link TableIndex} to this preset.
     * @param index The {@link TableIndex} to add
     * @return This TablePreset
     * @see TableIndex
     */
    public TablePreset addIndex(TableIndex index) {
        indices.add(index);
        return this;
    }

    /**
     * Removes an index.
     * @param index The index to remove.
     * @return This TablePreset
     */
    public TablePreset removeIndex(TableIndex index) {
        indices.remove(index);
        return this;
    }

    /**
     * Add a foreign key to this preset.
     * @param foreignKey The foreign key to add
     * @return This TablePreset
     * @see ForeignKey
     */
    public TablePreset addForeignKey(ForeignKey foreignKey) {
        foreignKeys.add(foreignKey);
        return this;
    }

    /**
     * Add a foreign key to this preset.
     * @param foreignKey The foreign key to add
     * @return This TablePreset
     * @see ForeignKey
     */
    public TablePreset addForeignKey(ForeignKey.Builder foreignKey) {
        foreignKeys.add(foreignKey.build());
        return this;
    }

    /**
     * Remove a foreign key from this preset.
     * @param foreignKey The foreign key to remove
     * @return This TablePreset
     */
    public TablePreset removeForeignKey(ForeignKey foreignKey) {
        foreignKeys.remove(foreignKey);
        return this;
    }

    /**
     * Adds a new check constraint insertions and updates must comply with to be valid.
     * @param check The check constraint to add
     * @return This TablePreset
     * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html">MySQL :: MySQL 8.0 Reference Manual :: 13.1.20.6 CHECK Constraints</a>
     */
    public TablePreset addCheck(QueryCondition check) {
        checks.add(check);
        return this;
    }

    /**
     * Removes a check constraint
     * @param check The check constraint to remove
     * @return This TablePreset
     */
    public TablePreset removeCheck(QueryCondition check) {
        checks.remove(check);
        return this;
    }

    /**
     * @return An immutable copy of the map of columns in this preset
     */
    public Map<String, ColumnStructure<?>> getColumns() {
        return Collections.unmodifiableMap(columns);
    }

    /**
     * @return An immutable copy of the foreign keys added to this preset
     */
    public List<ForeignKey> getForeignKeys() {
        return Collections.unmodifiableList(foreignKeys);
    }

    /**
     * @return An immutable copy of the checks keys added to this preset
     */
    public List<QueryCondition> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    /**
     * Builds the type strings of all columns and maps them to their name.
     * @param type The type to build for
     * @return A map of column name keys and column definition values
     */
    public Map<String, String> build(Database.RDBMS type) {
        return columns.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString(type), (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Builds the query to create a table representing this preset.
     * @param type The type to build for
     * @return A built CREATE TABLE query
     */
    public String buildQuery(Database.RDBMS type) {
        if (getColumns().isEmpty())
            throw new IllegalStateException("Cannot create a table without columns.");

        return "CREATE TABLE IF NOT EXISTS " + getName() + " (" +
                // Columns
                build(type).entrySet().stream()
                        .map(entry -> Database.engrave(entry.getKey()) + " " + entry.getValue())
                        .collect(Collectors.joining(", ")) +
                // Indices
                // Indices do not work on SQLite apparently when creating a table.
                (getIndices().isEmpty() || type == Database.RDBMS.SQLite ? "" : ", " + getIndices().stream()
                        .map(TableIndex::toString)
                        .collect(Collectors.joining(", "))) +
                // Foreign keys
                (getForeignKeys().isEmpty() ? "" : ", " + getForeignKeys().stream()
                        .map(ForeignKey::toString)
                        .collect(Collectors.joining(", "))) +
                // Checks
                (getChecks().isEmpty() ? "" : ", " + getChecks().stream()
                        .map(check -> "CHECK (" + check + ")")
                        .collect(Collectors.joining(", "))) + ");";
    }

    /**
     * @return All indices that were added
     */
    public List<TableIndex> getIndices() {
        return Collections.unmodifiableList(indices);
    }

    /**
     * Builds a query and executes it on the given database.
     * @param db The database to create this table on
     * @return This TablePreset
     * @see #buildQuery(Database.RDBMS)
     * @see #createAsync(Database)
     */
    public TablePreset create(Database db) {
        db.createTable(this);
        return this;
    }

    /**
     * Builds a query and executes it on the given database asynchronously.
     * @param db The database to create this table on
     * @return This TablePreset
     * @see #buildQuery(Database.RDBMS)
     * @see #create(Database)
     */
    public CompletableFuture<TablePreset> createAsync(Database db) {
        return db.createTableAsync(this).thenApply(v -> this);
    }

    /**
     * @return A (deep) copy of this TablePreset
     */
    @Override
    public TablePreset clone() {
        TablePreset clone = new TablePreset(name);
        clone.putAll(columns.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().clone())));
        clone.indices.addAll(indices.stream().map(TableIndex::clone).collect(Collectors.toList()));
        clone.foreignKeys.addAll(foreignKeys);
        clone.checks.addAll(checks);
        return clone;
    }
}
