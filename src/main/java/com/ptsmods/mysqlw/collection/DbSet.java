package com.ptsmods.mysqlw.collection;

import com.google.common.base.Preconditions;
import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.QueryConditions;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TablePreset;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiFunction;

public class DbSet<E> extends AbstractSet<E> implements DbCollection {

    private static final TablePreset preset = TablePreset.create("set_").putColumn("value", ColumnType.VARCHAR.createStructure().satiateSupplier(sup -> sup.apply(255)).setPrimary(true).setNullAllowed(false).setUnique(true));
    private static final Map<String, DbSet<?>> cache = new HashMap<>();
    private final Database db;
    private final String table;
    private final String name;
    private final BiFunction<E, DbCollection, String> elementToString;
    private final BiFunction<String, DbCollection, E> elementFromString;

    /**
     * Parses a String representation of a DbSet into a DbSet.
     * @param db The database this set belongs to. Used when creating a new set.
     * @param s The String to parse.
     * @param elementToString The function used to convert an element of this set into a String. Used when creating a new set.
     * @param elementFromString The function used to convert an element of this set into a String. Used when creating a new set.
     * @param <E> The type of the elements in this set.
     * @return A new DbSet or a cached one if available.
     */
    public static <E> DbSet<E> parseString(Database db, String s, BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        return s.startsWith("DbSet[name=") ? getSet(db, Database.readQuotedString(s.substring("DbSet[name=".length())), elementToString, elementFromString) : null;
    }

    /**
     * Gets a set from cache or creates a new one.
     * @param db The database this set belongs to. Used when creating a new map.
     * @param name The name of this set.
     * @param type The Class of type E if you've registered a type converter on {@link DbCF}. Used when creating a new set.
     * @param <E> The type of the elements in this set.
     * @return A new DbSet or a cached one if available.
     */
    public static <E> DbSet<E> getSet(Database db, String name, Class<E> type) {
        return getSet(db, name, DbCF.getTo(type), DbCF.getFrom(type));
    }

     /**
     * Gets a set from cache or creates a new one.
     * @param db The database this map belongs to. Used when creating a new map.
     * @param name The name of this set.
     * @param elementToString The function used to convert an element of this set into a String. Used when creating a new set.
     * @param elementFromString The function used to convert an element of this set into a String. Used when creating a new set.
     * @param <E> The type of the elements in this set.
     * @return A new DbSet or a cached one if available.
     */
    public static <E> DbSet<E> getSet(Database db, String name, BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        if (cache.containsKey(name))
            try {
                return (DbSet<E>) cache.get(name);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Wrong type! Cached DbList with the given name has a different type than requested.", e);
            }
        else return new DbSet<>(db, name, elementToString, elementFromString);
    }

    private DbSet(Database db, String name, BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        if (cache.containsKey(name)) throw new IllegalArgumentException("A DbList by this name already exists.");
        Preconditions.checkNotNull(db, "database");
        Preconditions.checkNotNull(elementToString, "elementToString");
        Preconditions.checkNotNull(elementFromString, "elementFromString");
        this.db = db;
        this.table = "set_" + name;
        this.name = name;
        preset.setName(table).create(db);
        this.elementToString = elementToString;
        this.elementFromString = elementFromString;
        cache.put(name, this);
    }

    @Override
    public int size() {
        return db.select(table, "value", null, null).size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return db.select(table, "value", QueryCondition.equals("value", elementToString.apply((E) o, this)), null).size() > 0;
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return toHashSet().iterator();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return toHashSet().toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(T[] a) {
        return toHashSet().toArray(a);
    }

    @Override
    public boolean add(E e) {
        return db.insertIgnore(table, "value", elementToString.apply(e, this)) > 0;
    }

    @Override
    public boolean remove(Object o) {
        return db.delete(table, QueryCondition.equals("value", elementToString.apply((E) o, this))) > 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        for (Object element : c)
            condition.or(QueryCondition.equals("value", elementToString.apply((E) element, this)));
        return db.select(table, new String[] {"value"}, condition, null).size() == c.size();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        List<Object[]> values = new ArrayList<>();
        c.forEach(e -> values.add(new Object[] {elementToString.apply(e, this)}));
        return db.replace(table, new String[] {"value"}, values) > 0;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        c.forEach(o -> condition.and(QueryCondition.notEquals("value", elementToString.apply((E) o, this))));
        return db.delete(table, condition) > 0;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        c.forEach(o -> condition.or(QueryCondition.equals("value", elementToString.apply((E) o, this))));
        return db.delete(table, condition) > 0;
    }

    @Override
    public void clear() {
        db.truncate(table);
    }

    @Override
    public Database getDb() {
        return db;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DbSet[name='" + getName() + "',values=" + super.toString() + "]";
    }

    public Set<E> toHashSet() {
        Set<E> set = new HashSet<>();
        db.select(table, "value", null, null).forEach(row -> set.add(elementFromString.apply(row.get("value").toString(), this)));
        return set;
    }
}
