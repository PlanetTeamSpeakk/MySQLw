package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.table.configurable.Defaultable;
import com.ptsmods.mysqlw.table.configurable.SimpleConfigurable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A structure used to describe a column.<br>
 * Used when making new tables.<br>
 * Acquire a {@link ColumnStructure} via {@link ColumnType#struct()} on any of the types in that class.
 * @param <C> The type of the configurable used to get the typeString of the {@link ColumnType} used to create this structure.
 */
public class ColumnStructure<C> {
    private final ColumnType<C> type;
    private Function<C, String> configurator = null;
    private String typeString = null;
    private boolean unique = false;
    private boolean primary = false;
    private ColumnDefault defValue = null;
    private ColumnAttributes attributes = null;
    private boolean nullAllowed = true;
    private boolean autoIncrement = false;
    private String comment = null;
    private String extra = null;
    private boolean readOnly = false;

    ColumnStructure(ColumnType<C> type) {
        this.type = type;
    }

    public ColumnType<C> getType() {
        return type;
    }

    /**
     * Basically what configuring does, except you put in raw data.<br>
     * Don't forget to include the type in here too.<br>
     * Example: VARCHAR(255)
     * @param typeString The new typeString.
     * @return This structure
     */
    public ColumnStructure<C> setTypeString(String typeString) {
        this.typeString = typeString;
        return this;
    }

    /**
     * @return The string set using {@link #configure(Function)}.
     * Will return {@code null} unless {@link #setTypeString(String)} has been called.
     */
    @Nullable
    public String getTypeString() {
        return typeString;
    }

    /**
     * Run and return the value of the supplier of the selected {@link ColumnType}.
     * <p style="font-weight: bold; color: red;">THIS MUST BE RAN, UNLESS THE SUPPLIER IS AN INSTANCE OF {@link Supplier}.</p>
     * @param configurator The function that gets the supplier and returns its value.
     * @return This structure
     * @deprecated Has been renamed. Use {@link #configure(Function)} instead.
     */
    @Deprecated
    public ColumnStructure<C> satiateSupplier(Function<C, String> configurator) {
        return configure(configurator);
    }

    /**
     * Run and return the value of the supplier of the selected {@link ColumnType}.<br><br>
     * This <b>must</b> be called if the {@link ColumnType} of this structure has no default values set for its supplier.<br>
     * (This is the case for e.g. {@link ColumnType#ENUM ENUM}, {@link ColumnType#SET SET}, {@link ColumnType#CHAR CHAR},
     * {@link ColumnType#VARCHAR VARCHAR})
     * @param configurator The function that gets the supplier and returns its value.
     * @return This structure
     */
    public ColumnStructure<C> configure(Function<C, String> configurator) {
        checkRO();
        this.configurator = configurator;
        return this;
    }

    /**
     * @return The supplier that's used to configure this structure.
     * @see #configure(Function)
     * @deprecated Suppliers are now type-specific, please use {@link #getTypeSpecificSupplier()} instead.
     */
    @Deprecated
    public C getSupplier() {
        return type.getSupplier();
    }

    /**
     * @return The supplier that may return a different output based on the RDBMS it's used for.
     * Used to configure this structure.
     */
    public Function<Database.RDBMS, C> getTypeSpecificSupplier() {
        return type.getTypeSpecificSupplier();
    }

    /**
     * Sets unique to true.
     * @return This structure
     * @see #setUnique(boolean)
     */
    public ColumnStructure<C> setUnique() {
        return setUnique(true);
    }

    /**
     * @param unique Whether this column may only contain unique values.
     * @return This structure
     */
    public ColumnStructure<C> setUnique(boolean unique) {
        checkRO();
        this.unique = unique;
        return this;
    }

    /**
     * @return Whether this column may only contain unique values.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Sets primary to true.
     * @return This structure
     * @see #setPrimary(boolean) 
     */
    public ColumnStructure<C> setPrimary() {
        return setPrimary(true);
    }

    /**
     * @param primary Whether this column is the PRIMARY KEY of its table.
     * @return This structure
     */
    public ColumnStructure<C> setPrimary(boolean primary) {
        checkRO();
        this.primary = primary;
        return this;
    }

    /**
     * @return Whether this column is the PRIMARY KEY of its table.
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * @param defValue The default value of this column, either {@link ColumnDefault#NULL NULL},
     * {@link ColumnDefault#CURRENT_TIMESTAMP CURRENT_TIMESTAMP} or a custom default value.
     * @return This structure
     */
    public ColumnStructure<C> setDefault(@Nullable ColumnDefault defValue) {
        checkRO();
        if (defValue != null && defValue.getDef().equals(ColumnDefault.NULL.getDef()) && !nullAllowed)
            throw new IllegalArgumentException("Default value may not be NULL when null is not allowed.");
        this.defValue = defValue;
        return this;
    }

    /**
     * @return The default value of this column, either {@link ColumnDefault#NULL NULL}, {@link ColumnDefault#CURRENT_TIMESTAMP CURRENT_TIMESTAMP} or a custom default value.
     */
    public ColumnDefault getDefValue() {
        return defValue;
    }

    /**
     * @param attributes The attributes of the type of this column.
     * @return This structure
     */
    public ColumnStructure<C> setAttributes(@Nullable ColumnAttributes attributes) {
        checkRO();
        this.attributes = attributes;
        return this;
    }

    /**
     * @return The attributes of the type of this column.
     */
    public ColumnAttributes getAttributes() {
        return attributes;
    }

    /**
     * Sets null allowed to true.
     * @return This structure
     * @see #setNullAllowed(boolean)
     */
    public ColumnStructure<C> setNullable() {
        return setNullAllowed(true);
    }

    /**
     * Sets null allowed to false.
     * @return This structure
     * @see #setNullAllowed(boolean)
     */
    public ColumnStructure<C> setNonNull() {
        return setNullAllowed(false);
    }

    /**
     * @param nullAllowed Whether this column can contain null values.
     * @return This structure
     */
    public ColumnStructure<C> setNullAllowed(boolean nullAllowed) {
        checkRO();
        this.nullAllowed = nullAllowed;
        return this;
    }

    /**
     * @return Whether this column can contain null values.
     */
    public boolean isNullAllowed() {
        return nullAllowed;
    }

    /**
     * Sets auto increment to true.
     * @return This structure.
     * @see #setAutoIncrement(boolean) 
     */
    public ColumnStructure<C> setAutoIncrement() {
        return setAutoIncrement(true);
    }
    
    /**
     * @param autoIncrement Whether the value of this column should be incremented by one for each row inserted.
     * @return This structure.
     */
    public ColumnStructure<C> setAutoIncrement(boolean autoIncrement) {
        checkRO();
        this.autoIncrement = autoIncrement;
        return this;
    }

    /**
     * @return Whether the value of this column should be incremented by one for each row inserted.
     */
    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    /**
     * @param comment The comment of this column. Used to describe what it's for.
     * @return This structure
     */
    public ColumnStructure<C> setComment(@Nullable String comment) {
        checkRO();
        this.comment = comment;
        return this;
    }

    /**
     * @return The comment of this column. Used to describe what it's for.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param extra Anything else you could possibly want to add that this class does not cover.
     *              It would also be appreciated if you could make a pull request or issue to
     *              cover this on <a href="https://github.com/PlanetTeamSpeakk/MySQLw">the GitHub page</a>.
     * @return This structure
     */
    public ColumnStructure<C> setExtra(@Nullable String extra) {
        checkRO();
        this.extra = extra;
        return this;
    }

    /**
     * @return Anything else you could possibly want to add that this class does not cover.
     */
    public String getExtra() {
        return extra;
    }

    /**
     * Makes this ColumnStructure immutable so it cannot be edited.<br>
     * Used when describing a table.
     * @return This ColumnStructure
     */
    public ColumnStructure<C> readOnly() {
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
    public ColumnStructure<C> clone() {
        ColumnStructure<C> clone = new ColumnStructure<>(type);
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
        C configurable = getTypeSpecificSupplier().apply(type);
        if (configurator == null && !(configurable instanceof SimpleConfigurable) &&
                (!(configurable instanceof Defaultable<?, ?>) || !((Defaultable<?, ?>) configurable).hasDefault()))
            throw new IllegalStateException("Structure has not yet been configured.");

        String typeString = configurator != null ? configurator.apply(configurable) :
                configurable instanceof SimpleConfigurable ? ((SimpleConfigurable) configurable).get() :
                ((Defaultable<?, ?>) configurable).applyDefault();

        StringBuilder builder = new StringBuilder(typeString);
        if (attributes != null) builder.append(' ').append(attributes);
        if (primary) builder.append(" PRIMARY KEY");
        if (autoIncrement) builder.append(type == Database.RDBMS.SQLite ? " AUTOINCREMENT" : " AUTO_INCREMENT");
        if (unique) builder.append(" UNIQUE");
        if (defValue != null) builder.append(" DEFAULT ").append(defValue.getDefString());
        builder.append(nullAllowed || defValue == ColumnDefault.NULL ? " NULL" : " NOT NULL");
        if (comment != null) builder.append(" COMMENT ").append(Database.enquote(comment));
        if (extra != null) builder.append(" ").append(extra);
        return builder.toString();
    }
}
