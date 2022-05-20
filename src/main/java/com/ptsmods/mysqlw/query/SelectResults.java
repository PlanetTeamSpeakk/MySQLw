package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Returned when you use any of the select methods in {@link Database}.
 * Contains all data you need.
 */
@SuppressWarnings("unused")
public class SelectResults implements List<SelectResults.SelectResultRow> {

    private final Database db;
    private final String table;
    private final QueryCondition condition;
    private final QueryOrder order;
    private final QueryLimit limit;
    private final List<String> columns;
    private final List<SelectResultRow> data;

    /**
     * Parse a ResultSet into a SelectResults object.
     * @param set The ResultSet to parse.
     * @return The SelectResults object that was parsed from this set.
     * @see #parse(Database, String, ResultSet, QueryCondition, QueryOrder, QueryLimit)
     */
    public static SelectResults parse(ResultSet set) {
        return parse(null, null, set, null, null, null);
    }

    /**
     * Parse a ResultSet into a SelectResults object.
     * @param db The Database this ResultSet was created with.
     * @param table The table this ResultSet contains rows of.
     * @param set The ResultSet to parse.
     * @param condition The condition used when getting this ResultSet.
     * @param order The order in which the rows of this ResultSet are sorted.
     * @param limit The maximum amount of rows this ResultSet can contain.
     * @return The SelectResults object that was parsed from this set.
     */
    public static SelectResults parse(Database db, String table, ResultSet set, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        List<String> columns = new ArrayList<>();
        boolean columnsFilled = false;
        List<Map<String, Object>> result = new ArrayList<>();
        if (set != null)
            try {
                while (set.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= set.getMetaData().getColumnCount(); i++) {
                        row.put(set.getMetaData().getColumnName(i), set.getObject(i));
                        if (!columnsFilled) columns.add(set.getMetaData().getColumnName(i));
                    }
                    columnsFilled = true;
                    result.add(row);
                }
                set.getStatement().close();
            } catch (SQLException e) {
                db.logOrThrow("Error iterating through results from table '" + table + "'.", e);
            }
        return new SelectResults(db, table, columns, condition, order, limit, result);
    }

    private SelectResults(Database db, String table, List<String> columns, QueryCondition condition, QueryOrder order, QueryLimit limit, List<Map<String, Object>> data) {
        this.db = db;
        this.table = table;
        this.condition = condition;
        this.order = order;
        this.limit = limit;
        this.columns = Collections.unmodifiableList(columns);
        this.data = Collections.unmodifiableList(data.stream().map(SelectResultRow::new).collect(Collectors.toList()));
    }

    /**
     * @return The database the query used to get these results was run on.
     */
    public Database getDb() {
        return db;
    }

    /**
     * @return The table the query used to get these results was run on.
     */
    public String getTable() {
        return table;
    }

    /**
     * @return A list of all columns present in these SelectResults.
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * @return The condition all rows in these results meet.
     */
    public QueryCondition getCondition() {
        return condition;
    }

    /**
     * @return The order in which the rows are sorted.
     */
    public QueryOrder getOrder() {
        return order;
    }

    /**
     * @return The limit of rows returned, including the offset at which these rows are selected from the entire result.
     */
    public QueryLimit getLimit() {
        return limit;
    }

    /**
     * @return A list of all rows in these results.
     * @see #get(int)
     */
    public List<SelectResultRow> getRows() {
        return data;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return data.contains(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectResults)) return false;
        SelectResults that = (SelectResults) o;
        return getDb().equals(that.getDb()) && getTable().equals(that.getTable()) && getColumns().equals(that.getColumns()) && getRows().equals(that.getRows());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDb(), getTable(), getColumns(), getRows());
    }

    @Override
    public SelectResultRow get(int index) {
        return data.get(index);
    }

    private void throwException() {
        throw new UnsupportedOperationException("SelectResults cannot be altered.");
    }

    @Override
    public SelectResultRow set(int index, SelectResultRow element) {
        throwException();
        return null;
    }

    @Override
    public void add(int index, SelectResultRow element) {
        throwException();
    }

    @Override
    public SelectResultRow remove(int index) {
        throwException();
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return data.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return data.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<SelectResultRow> listIterator() {
        return data.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<SelectResultRow> listIterator(int index) {
        return data.listIterator(index);
    }

    @NotNull
    @Override
    public List<SelectResultRow> subList(int fromIndex, int toIndex) {
        return data.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return "SelectResults[" +
                "db=" + db +
                ", table='" + table + '\'' +
                ", condition='" + condition + '\'' +
                ", order='" + order + '\'' +
                ", columns=" + columns +
                ", data=" + data +
                ']';
    }

    @NotNull
    @Override
    public Iterator<SelectResultRow> iterator() {
        return data.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return data.toArray(a);
    }

    @Override
    public boolean add(SelectResultRow selectResultRow) {
        throwException();
        return false;
    }

    @Override
    public boolean remove(Object o) {
        throwException();
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return data.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends SelectResultRow> c) {
        throwException();
        return false;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends SelectResultRow> c) {
        throwException();
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throwException();
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throwException();
        return false;
    }

    @Override
    public void clear() {
        throwException();
    }

    /**
     * A row in {@link SelectResults}
     */
    public class SelectResultRow implements Map<String, Object> {

        private final Map<String, Object> data;

        private SelectResultRow(Map<String, Object> data) {
            this.data = Collections.unmodifiableMap(data);
        }

        /**
         * @return A list of all columns in this row.
         */
        public List<String> getColumns() {
            return columns;
        }

        @Override
        public String toString() {
            return "SelectResultRow[" +
                    "data=" + data +
                    ']';
        }

        @Override
        public int size() {
            return columns.size();
        }

        @Override
        public boolean isEmpty() {
            return columns.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return key instanceof String && columns.contains(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return data.containsValue(value);
        }

        @Override
        public Object get(Object column) {
            if (!(column instanceof String) || !getColumns().contains(column)) throw new IllegalArgumentException("No column by that name exists.");
            else return data.get(column);
        }

        public String getString(String column) {
            return (String) get(column);
        }

        public Number getNumber(String column) {
            return (Number) get(column);
        }

        public byte getByte(String column) {
            return getNumber(column).byteValue();
        }

        public short getShort(String column) {
            return getNumber(column).shortValue();
        }

        public int getInt(String column) {
            return getNumber(column).intValue();
        }

        public long getLong(String column) {
            return getNumber(column).longValue();
        }

        public float getFloat(String column) {
            return getNumber(column).floatValue();
        }

        public double getDouble(String column) {
            return getNumber(column).doubleValue();
        }

        public Timestamp getTimestamp(String column) {
            // If it's not a Timestamp or String, you're probably doing something wrong.
            // (SQLite likes to send these as a String instead and I'm assuming that goes for the following types too)
            return get(column) instanceof Timestamp || get(column) == null ? (Timestamp) get(column) : Timestamp.valueOf(getString(column));
        }

        public Date getDate(String column) {
            return get(column) instanceof Date || get(column) == null ? (Date) get(column) : Date.valueOf(getString(column));
        }

        public Time getTime(String column) {
            return get(column) instanceof Time || get(column) == null ? (Time) get(column) : Time.valueOf(getString(column));
        }

        public byte[] getByteArray(String column) {
            // I believe BLOB type columns are returned this way.
            // And I know geometry types are.
            return (byte[]) get(column);
        }

        public Blob getBlob(String column) {
            // Don't actually know if MySQL or SQLite uses this, but in case they do, here you go.
            return (Blob) get(column);
        }

        /**
         * Returns a type registered using {@link Database#registerTypeConverter(Class, Function, Function)}.
         * @param column The column to get the object from.
         * @param type The class of the object.
         * @param <T> The generic type of the object.
         * @return The object in this column.
         */
        public <T> T get(String column, Class<T> type) {
            return Database.getFromString(getString(column), type);
        }

        private void throwException() {
            throw new UnsupportedOperationException("SelectResult cannot be altered.");
        }

        @Nullable
        @Override
        public Object put(String key, Object value) {
            throwException();
            return null;
        }

        @Override
        public Object remove(Object key) {
            throwException();
            return null;
        }

        @Override
        public void putAll(@NotNull Map<? extends String, ?> m) {
            throwException();
        }

        @Override
        public void clear() {
            throwException();
        }

        @NotNull
        @Override
        public Set<String> keySet() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(columns));
        }

        @NotNull
        @Override
        public Collection<Object> values() {
            return data.values();
        }

        @NotNull
        @Override
        public Set<Entry<String, Object>> entrySet() {
            return data.entrySet();
        }
    }

}
