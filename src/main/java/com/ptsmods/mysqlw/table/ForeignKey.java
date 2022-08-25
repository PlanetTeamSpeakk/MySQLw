package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * An indication that every row in a table references to a row in another table (child/parent rows)
 */
@Getter
@Builder(builderClassName = "Builder")
public class ForeignKey {
    @NonNull
    private final String column;
    @NonNull
    private final String referenceTable;
    @NonNull
    private final String referenceColumn;
    @NonNull // Use NO_ACTION for no action
    @lombok.Builder.Default
    private Action onDelete = Action.NO_ACTION, onUpdate = Action.NO_ACTION;

    /**
     * Create a new foreign key without an onDelete or onUpdate action.
     * @param column The name of the column that contains the foreign key
     * @param referenceTable The table that contains the rows this foreign key references to
     * @param referenceColumn The column of the primary key of that table
     * @return A new Foreign Key
     * @see #builder()
     */
    public static ForeignKey foreignKey(String column, String referenceTable, String referenceColumn) {
        return builder()
                .column(column)
                .referenceTable(referenceTable)
                .referenceColumn(referenceColumn)
                .build();
    }

    @Override
    public String toString() {
        return "FOREIGN KEY (" + Database.engrave(getColumn()) + ") REFERENCES " +
                Database.engrave(getReferenceTable()) + "(" + Database.engrave(getReferenceColumn()) + ")" +
                " ON DELETE " + getOnDelete() + " ON UPDATE " + getOnUpdate();
    }

    public enum Action {
        NO_ACTION, RESTRICT, SET_NULL, CASCADE
    }
}
