package com.ptsmods.mysqlw.table;

/**
 * Table indices, used to speed up queries.
 */
public class TableIndexType {

    public static com.ptsmods.mysqlw.table.TableIndexType index(String name, String column, Type type) {
        return new com.ptsmods.mysqlw.table.TableIndexType(name, column, type);
    }

    public static com.ptsmods.mysqlw.table.TableIndexType index(String column, Type type) {
        return new com.ptsmods.mysqlw.table.TableIndexType(null, column, type);
    }

    private final String name, column;
    private final Type type;

    private TableIndexType(String name, String column, Type type) {
        this.name = name;
        this.column = column;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getColumn() {
        return column;
    }

    public Type getType() {
        return type;
    }

    @Override
    public com.ptsmods.mysqlw.table.TableIndexType clone() {
        return new com.ptsmods.mysqlw.table.TableIndexType(name, column, type);
    }

    @Override
    public String toString() {
        return type.toString(name, column);
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
         * Indexes for {@link ColumnType#GEOMETRY GEOMETRY} objects.
         */
        SPATIAL;

        public String toString(String name, String column) {
            return (this == INDEX ? "" : name() + " ") + "INDEX " + (name == null ? "" : "`" + name + "` ") + "(`" + column + "`)";
        }
    }
}
