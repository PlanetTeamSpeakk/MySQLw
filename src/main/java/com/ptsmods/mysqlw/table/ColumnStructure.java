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
    private ColumnDefault defValue = ColumnDefault.NULL;
    private ColumnAttributes attributes = null;
    private boolean nullAllowed = true;
    private boolean autoIncrement = false;
    private String comment = null;
    private String extra = null;
    private boolean readOnly = false;

	@SuppressWarnings("unchecked") // Any ColumnType that has a Supplier as supplier is of generic type String.
    ColumnStructure(ColumnType<S> type) {
        this.type = type;
        if (getSupplier() instanceof Supplier) typeString = ((Supplier<String>) getSupplier()).get();
    }

    public ColumnType<S> getType() {
        return type;
    }

    /**
     * Basically what configuring does, except you put in raw data.<br>
     * Don't forget to include the type in here too.<br>
     * Example: VARCHAR(255)
     * @param typeString The new typeString.
     * @return This structure
     */
    public ColumnStructure<S> setTypeString(String typeString) {
        this.typeString = typeString;
        return this;
    }

	/**
	 * Run and return the value of the supplier of the selected {@link ColumnType}.
	 * <p style="font-weight: bold; color: red;">THIS MUST BE RAN, UNLESS THE SUPPLIER IS AN INSTANCE OF {@link Supplier}.</p>
	 * @param configurator The function that gets the supplier and returns its value.
	 * @return This structure
	 * @deprecated Has been renamed. Use {@link #configure(Function)} instead.
	 */
	@Deprecated
	public ColumnStructure<S> satiateSupplier(Function<S, String> configurator) {
		return configure(configurator);
	}

    /**
     * Run and return the value of the supplier of the selected {@link ColumnType}.
     * <p style="font-weight: bold; color: red;">THIS MUST BE RAN, UNLESS THE SUPPLIER IS AN INSTANCE OF {@link Supplier}.</p>
     * @param configurator The function that gets the supplier and returns its value.
     * @return This structure
     */
    public ColumnStructure<S> configure(Function<S, String> configurator) {
        checkRO();
        typeString = configurator.apply(getSupplier());
        return this;
    }

    /**
     * @return The supplier that's used to configure this structure.
     * @see #configure(Function)
     */
    public S getSupplier() {
        return type.getSupplier();
    }

    /**
     * @param unique Whether this column should only contain unique values.
     * @return This structure
     */
    public ColumnStructure<S> setUnique(boolean unique) {
        checkRO();
        this.unique = unique;
        return this;
    }

    /**
     * @param primary Whether this column is the PRIMARY KEY of its table.
     * @return This structure
     */
    public ColumnStructure<S> setPrimary(boolean primary) {
        checkRO();
        this.primary = primary;
        return this;
    }

    /**
     * @param defValue The default value of this column, either {@link ColumnDefault#NULL NULL}, {@link ColumnDefault#CURRENT_TIMESTAMP CURRENT_TIMESTAMP} or a custom default value.
     * @return This structure
     */
    public ColumnStructure<S> setDefault(@Nullable ColumnDefault defValue) {
        checkRO();
		defValue = defValue == null ? ColumnDefault.NULL : defValue;
		if (defValue.getDef().equals(ColumnDefault.NULL.getDef()) && !nullAllowed)
			throw new IllegalArgumentException("Default value may not be NULL when null is not allowed.");
        this.defValue = defValue;
        return this;
    }

    /**
     * @param attributes The attributes of the type of this column.
     * @return This structure
     */
    public ColumnStructure<S> setAttributes(@Nullable ColumnAttributes attributes) {
        checkRO();
        this.attributes = attributes;
        return this;
    }

    /**
     * @param nullAllowed Whether this column can contain null values.
     * @return This structure
     */
    public ColumnStructure<S> setNullAllowed(boolean nullAllowed) {
        checkRO();
        this.nullAllowed = nullAllowed;
        return this;
    }

    /**
     * @param autoIncrement Whether the value of this column should be incremented by one for each row inserted.
     * @return This structure.
     */
    public ColumnStructure<S> setAutoIncrement(boolean autoIncrement) {
        checkRO();
        this.autoIncrement = autoIncrement;
        return this;
    }

    /**
     * @param comment The comment of this column. Used to describe what it's for.
     * @return This structure
     */
    public ColumnStructure<S> setComment(@Nullable String comment) {
        checkRO();
        this.comment = comment;
        return this;
    }

    /**
     * @param extra Anything else you could possibly want to add that this class does not cover.
	 *              It would also be appreciated if you could make a pull request or issue to
	 *              cover this on <a href="https://github.com/PlanetTeamSpeakk/MySQLw">the GitHub page</a>.
     * @return This structure
     */
    public ColumnStructure<S> setExtra(@Nullable String extra) {
        checkRO();
        this.extra = extra;
        return this;
    }

    /**
     * Makes this ColumnStructure immutable so it cannot be edited.<br>
     * Used when describing a table.
     * @return This ColumnStructure
     */
    public ColumnStructure<S> readOnly() {
        readOnly = true;
        return this;
    }

    private void checkRO() {
        if (readOnly) throw new IllegalArgumentException("This ColumnStructure is immutable and can thus not be edited");
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
        return toString(Database.RDBMS.UNKNOWN);
    }

    public String toString(Database.RDBMS type) {
        if (typeString == null) throw new IllegalStateException("Structure has not yet been configured.");
		if (defValue.getDef().equals(ColumnDefault.NULL.getDef()) && !nullAllowed) throw new IllegalStateException("Default value may not be NULL when null is not allowed.");

        StringBuilder builder = new StringBuilder(typeString);
        if (attributes != null) builder.append(' ').append(attributes);
        if (primary) builder.append(" PRIMARY KEY");
        if (autoIncrement) builder.append(type == Database.RDBMS.SQLite ? " AUTOINCREMENT" : " AUTO_INCREMENT");
        if (unique) builder.append(" UNIQUE");
        builder.append(" DEFAULT ").append(defValue.getDefString());
        builder.append(nullAllowed || defValue == ColumnDefault.NULL ? " NULL" : " NOT NULL");
        if (comment != null) builder.append(" COMMENT ").append(Database.enquote(comment));
        if (extra != null) builder.append(" ").append(extra);
        return builder.toString();
    }
}
