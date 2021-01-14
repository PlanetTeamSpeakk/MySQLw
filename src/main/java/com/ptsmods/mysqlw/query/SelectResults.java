package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Returned when you use any of the select methods in {@link Database}.
 * Contains all data you need.
 */
public class SelectResults implements List<SelectResults.SelectResultRow> {

    private final Database db;
    private final String table;
    private final QueryCondition condition;
    private final QueryOrder order;
    private final List<String> columns;
    private final List<SelectResultRow> data;

    public SelectResults(Database db, String table, CharSequence[] columns, QueryCondition condition, QueryOrder order, List<Map<String, Object>> data) {
        this.db = db;
        this.table = table;
        this.columns = Database.convertList(ImmutableList.copyOf(columns), String::valueOf);
        this.condition = condition;
        this.order = order;
        this.data = ImmutableList.copyOf(Database.convertList(data, SelectResultRow::new));
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
     * @return A list of all rows in these results.
     * @see #get(int)
     */
    public List<SelectResultRow> getRows() {
        return ImmutableList.copyOf(data);
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

    @Nonnull
    @Override
    public ListIterator<SelectResultRow> listIterator() {
        return data.listIterator(); // Data is an ImmutableList, so it cannot be edited via this iterator either.
    }

    @Nonnull
    @Override
    public ListIterator<SelectResultRow> listIterator(int index) {
        return data.listIterator(index);
    }

    @Nonnull
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

    @Nonnull
    @Override
    public Iterator<SelectResultRow> iterator() {
        return data.iterator();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
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
    public boolean containsAll(@Nonnull Collection<?> c) {
        return data.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends SelectResultRow> c) {
        throwException();
        return false;
    }

    @Override
    public boolean addAll(int index, @Nonnull Collection<? extends SelectResultRow> c) {
        throwException();
        return false;
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        throwException();
        return false;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
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
            this.data = ImmutableMap.copyOf(data);
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
            else return data.getOrDefault(column, null);
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
        public void putAll(@Nonnull Map<? extends String, ?> m) {
            throwException();
        }

        @Override
        public void clear() {
            throwException();
        }

        @Nonnull
        @Override
        public Set<String> keySet() {
            return ImmutableSet.copyOf(columns);
        }

        @Nonnull
        @Override
        public Collection<Object> values() {
            return data.values();
        }

        @Nonnull
        @Override
        public Set<Entry<String, Object>> entrySet() {
            return data.entrySet();
        }
    }

}
