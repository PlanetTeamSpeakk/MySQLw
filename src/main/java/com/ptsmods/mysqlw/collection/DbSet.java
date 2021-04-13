package com.ptsmods.mysqlw.collection;

import com.google.common.base.Preconditions;
import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.QueryConditions;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TablePreset;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DbSet<E> extends AbstractSet<E> implements DbCollection {
    private static final TablePreset preset = TablePreset.create("set_").putColumn("value", ColumnType.VARCHAR.createStructure().satiateSupplier(sup -> sup.apply(255)).setPrimary(true).setNullAllowed(false).setUnique(true));
    private static final Map<String, DbSet<?>> cache = new HashMap<>();
    private final Database db;
    private final String table;
    private final String name;
    private final BiFunction<E, DbCollection, String> elementToString;
    private final BiFunction<String, DbCollection, E> elementFromString;
    private Executor executor;

    /**
     * Parses a String representation of a DbSet into a DbSet.
     * @param db The database this set belongs to. Used when creating a new set.
     * @param s The String to parse.
     * @param elementToString The function used to convert an element of this set into a String. Used when creating a new set.
     * @param elementFromString The function used to convert an element of this set into a String. Used when creating a new set.
     * @param <E> The type of the elements in this set.
     * @return A new DbSet or a cached one if available.
     */
    public static <E> DbSet<E> parseString(@Nonnull Database db, @Nonnull String s, @Nonnull BiFunction<E, DbCollection, String> elementToString, @Nonnull BiFunction<String, DbCollection, E> elementFromString) {
        return s.startsWith("DbSet[name=") ? getSet(db, Objects.requireNonNull(Database.readQuotedString(s.substring("DbSet[name=".length()))), elementToString, elementFromString) : null;
    }

    /**
     * Gets a set from cache or creates a new one.
     * @param db The database this set belongs to. Used when creating a new map.
     * @param name The name of this set.
     * @param type The Class of type E if you've registered a type converter on {@link DbCF}. Used when creating a new set.
     * @param <E> The type of the elements in this set.
     * @return A new DbSet or a cached one if available.
     */
    public static <E> DbSet<E> getSet(@Nonnull Database db, @Nonnull String name, @Nonnull Class<E> type) {
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
     @SuppressWarnings("unchecked")
    public static <E> DbSet<E> getSet(@Nonnull Database db, @Nonnull String name, @Nonnull BiFunction<E, DbCollection, String> elementToString, @Nonnull BiFunction<String, DbCollection, E> elementFromString) {
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
        // Not thread-safe so we use a fixed pool.
        executor = Executors.newFixedThreadPool(1, r -> new Thread(r, "Database Set Thread - " + name + ":" + db.getName()));
        cache.put(name, this);
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    public <T> CompletableFuture<T> runAsync(Supplier<T> sup) {
        return CompletableFuture.supplyAsync(sup, getExecutor());
    }

    public CompletableFuture<Void> runAsync(Runnable run) {
        return CompletableFuture.runAsync(run, getExecutor());
    }

    @Override
    public int size() {
        return db.count(table, "value", null);
    }

    @Nonnull
    public CompletableFuture<Integer> sizeAsync() {
        return runAsync(this::size);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Nonnull
    public CompletableFuture<Boolean> isEmptyAsync() {
        return runAsync(this::isEmpty);
    }

    @Override
    public boolean contains(Object o) {
        return db.select(table, "value", QueryCondition.equals("value", elementToString.apply((E) o, this)), null, null).size() > 0;
    }

    @Nonnull
    public CompletableFuture<Boolean> containsAsync(Object o) {
        return runAsync(() -> contains(o));
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return toHashSet().iterator();
    }

    @Nonnull
    public CompletableFuture<Iterator<E>> iteratorAsync() {
        return toHashSetAsync().thenApply(Set::iterator);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return toHashSet().toArray();
    }

    public CompletableFuture<Object[]> toArrayAsync() {
        return toHashSetAsync().thenApply(Set::toArray);
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        return toHashSet().toArray(a);
    }

    @Nonnull
    public <T> CompletableFuture<T[]> toArrayAsync(@Nonnull T[] a) {
        return toHashSetAsync().thenApply(l -> l.toArray(a));
    }

    @Override
    public boolean add(E e) {
        return db.insertIgnore(table, "value", elementToString.apply(e, this)) > 0;
    }

    public CompletableFuture<Boolean> addAsync(E e) {
        return runAsync(() -> add(e));
    }

    @Override
    public boolean remove(Object o) {
        return db.delete(table, QueryCondition.equals("value", elementToString.apply((E) o, this))) > 0;
    }

    public CompletableFuture<Boolean> removeAsync(Object o) {
        return runAsync(() -> remove(o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        for (Object element : c)
            condition.or(QueryCondition.equals("value", elementToString.apply((E) element, this)));
        return db.select(table, new String[] {"value"}, condition, null, null).size() == c.size();
    }

    public CompletableFuture<Boolean> containsAllAsync(@Nonnull Collection<?> c) {
        return runAsync(() -> containsAll(c));
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        List<Object[]> values = new ArrayList<>();
        c.forEach(e -> values.add(new Object[] {elementToString.apply(e, this)}));
        return db.replace(table, new String[] {"value"}, values) > 0;
    }

    public CompletableFuture<Boolean> addAllAsync(@Nonnull Collection<? extends E> c) {
        return runAsync(() -> addAll(c));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        c.forEach(o -> condition.and(QueryCondition.notEquals("value", elementToString.apply((E) o, this))));
        return db.delete(table, condition) > 0;
    }

    public CompletableFuture<Boolean> retainAllAsync(@Nonnull Collection<?> c) {
        return runAsync(() -> retainAll(c));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        c.forEach(o -> condition.or(QueryCondition.equals("value", elementToString.apply((E) o, this))));
        return db.delete(table, condition) > 0;
    }

    public CompletableFuture<Boolean> removeAllAsync(@Nonnull Collection<?> c) {
        return runAsync(() -> removeAll(c));
    }

    @Override
    public void clear() {
        db.truncate(table);
    }

    public CompletableFuture<Void> clearAsync() {
        return runAsync(this::clear);
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

    @Nonnull
    public Set<E> toHashSet() {
        return db.select(table, "value", null, null, null).stream().map(row -> elementFromString.apply(row.getString("value"), this)).collect(Collectors.toSet());
    }

    @Nonnull
    public CompletableFuture<Set<E>> toHashSetAsync() {
        return runAsync(this::toHashSet);
    }

    @Nonnull
    public BiFunction<E, DbCollection, String> getElementToString() {
        return elementToString;
    }

    @Nonnull
    public BiFunction<String, DbCollection, E> getElementFromString() {
        return elementFromString;
    }
}
