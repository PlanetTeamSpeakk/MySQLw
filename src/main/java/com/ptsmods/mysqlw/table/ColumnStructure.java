package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A structure used to describe a column.<br>
 * Used when making new tables.<br>
 * Acquire a {@link ColumnStructure} via {@link ColumnType#createStructure()} on any of the types in that class.
 * @param <S> The type of the supplier or functions used to get the typeString of the {@link ColumnType} used to create this structure.
 */
public class ColumnStructure<S> {

    private final ColumnType<S> type;
    private String typeString = null;
    private boolean unique = false;
    private boolean primary = false;
    private ColumnDefault defValue = null;
    private ColumnAttributes attributes = null;
    private boolean nullAllowed = true;
    private boolean autoIncrement = false;
    private String comment = null;
    private String extra = null;

    ColumnStructure(ColumnType<S> type) {
        this.type = type;
        if (getSupplier() instanceof Supplier) typeString = ((Supplier<String>) getSupplier()).get();
    }

    public ColumnType<S> getType() {
        return type;
    }

    /**
     * Run and return the value of the supplier of the selected {@link ColumnType}.
     * <p style="font-weight: bold; color: red; font-size: 25px;">THIS MUST BE RAN UNLESS THE SUPPLIER IS AN INSTANCE OF {@link Supplier}.</p>
     * @param run The function that gets the supplier and returns its value.
     * @return This structure
     */
    public ColumnStructure<S> satiateSupplier(Function<S, String> run) {
        typeString = run.apply(getSupplier());
        return this;
    }

    /**
     * @return The supplier that has to be satiated.
     * @see #satiateSupplier(Function)
     */
    public S getSupplier() {
        return type.getSupplier();
    }

    /**
     * @param unique Whether this column should only contain unique values.
     * @return This structure
     */
    public ColumnStructure<S> setUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    /**
     * @param primary Whether this column is the PRIMARY KEY of its table.
     * @return This structure
     */
    public ColumnStructure<S> setPrimary(boolean primary) {
        this.primary = primary;
        return this;
    }

    /**
     * @param defValue The default value of this column, either {@link ColumnDefault#NULL NULL} or {@link ColumnDefault#CURRENT_TIMESTAMP CURRENT_TIMESTAMP}.
     * @return This structure
     */
    public ColumnStructure<S> setDefault(@Nullable ColumnDefault defValue) {
        this.defValue = defValue;
        return this;
    }

    /**
     * @param attributes The attributes of the type of this column.
     * @return This structure
     */
    public ColumnStructure<S> setAttributes(@Nullable ColumnAttributes attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * @param nullAllowed Whether this column can contain null values.
     * @return This structure
     */
    public ColumnStructure<S> setNullAllowed(boolean nullAllowed) {
        this.nullAllowed = nullAllowed;
        return this;
    }

    /**
     * @param autoIncrement Whether the value of this column should be incremented by one for each row inserted.
     * @return This structure.
     */
    public ColumnStructure<S> setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
        return this;
    }

    /**
     * @param comment The comment of this column. Used to describe what it's for.
     * @return This structure
     */
    public ColumnStructure<S> setComment(@Nullable String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * @param extra Anything else you could possibly want to add that this class does not cover. It would also be appreciated if you could make a pull request or issue to cover this on <a href="https://github.com/PlanetTeamSpeakk/MySQLw">the GitHub page</a>.
     * @return This structure
     */
    public ColumnStructure<S> setExtra(@Nullable String extra) {
        this.extra = extra;
        return this;
    }

    /**
     * @return A shallow copy of this structure.
     */
    @Override
    public ColumnStructure<S> clone() {
        ColumnStructure<S> clone = new ColumnStructure<>(type);
        clone.typeString = typeString;
        clone.unique = unique;
        clone.primary = primary;
        clone.defValue = defValue;
        clone.attributes = attributes;
        clone.nullAllowed = nullAllowed;
        clone.autoIncrement = autoIncrement;
        clone.comment = comment;
        clone.extra = extra;
        return clone;
    }

    @Override
    public String toString() {
        if (typeString == null) throw new IllegalArgumentException("Supplier has not yet been satiated.");
        StringBuilder builder = new StringBuilder(typeString);
        if (attributes != null) builder.append(' ').append(attributes.toString());
        if (unique) builder.append(" UNIQUE");
        if (primary) builder.append(" PRIMARY KEY");
        if (defValue != null) builder.append(" DEFAULT ").append(defValue.name());
        builder.append(nullAllowed || defValue == ColumnDefault.NULL ? " NULL" : " NOT NULL");
        if (autoIncrement) builder.append(" AUTO_INCREMENT");
        if (comment != null) builder.append(" COMMENT ").append(Database.enquote(comment));
        if (extra != null) builder.append(" ").append(extra);
        return builder.toString();
    }
}
