package com.ptsmods.mysqlw.collection;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.QueryConditions;
import com.ptsmods.mysqlw.query.QueryOrder;
import com.ptsmods.mysqlw.query.SelectResults;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TablePreset;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.ptsmods.mysqlw.Database.checkNotNull;

@SuppressWarnings("unused")
public class DbList<E> extends AbstractList<E> implements DbCollection {

    private static final TablePreset preset = TablePreset.create("list_")
            .putColumn("id", ColumnType.INT.createStructure()
                    .configure(sup -> sup.apply(null))
                    .setAutoIncrement(true)
                    .setNullAllowed(false)
                    .setPrimary(true))
            .putColumn("val", ColumnType.TEXT.createStructure());
    private static final Map<String, DbList<?>> cache = new HashMap<>();
    private final Database db;
    private final String table;
    private final String name;
    private final BiFunction<E, DbCollection, String> elementToString;
    private final BiFunction<String, DbCollection, E> elementFromString;
    private Executor executor;

    /**
     * Parses a String representation of a DbList into a DbList.
     * @param db The database this list belongs to. Used when creating a new map.
     * @param s The String to parse.
     * @param elementToString The function used to convert an element of this list into a String. Used when creating a new list.
     * @param elementFromString The function used to convert an element of this list into a String. Used when creating a new list.
     * @param <E> The type of the elements in this set.
     * @return A new DbList or a cached one if available.
     */
    public static <E> DbList<E> parseString(Database db, String s, BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        return s.startsWith("DbList[name=") ? getList(db, Database.readQuotedString(s.substring("DbList[name=".length())), elementToString, elementFromString) : null;
    }

    /**
     * Parses a String representation of a DbList into a DbList.
     * @param db The database this list belongs to. Used when creating a new map.
     * @param name The name of this list.
     * @param type The Class of type E if you've registered a type converter on {@link DbCF}. Used when creating a new list.
     * @param <E> The type of the elements in this set.
     * @return A new DbList or a cached one if available.
     */
    public static <E> DbList<E> getList(Database db, String name, Class<E> type) {
        return getList(db, name, DbCF.getTo(type), DbCF.getFrom(type));
    }

    /**
     * Parses a String representation of a DbList into a DbList.
     * @param db The database this list belongs to. Used when creating a new map.
     * @param name The name of this list.
     * @param elementToString The function used to convert an element of this list into a String. Used when creating a new list.
     * @param elementFromString The function used to convert an element of this list into a String. Used when creating a new list.
     * @param <E> The type of the elements in this set.
     * @return A new DbList or a cached one if available.
     */
    @SuppressWarnings("unchecked")
    public static <E> DbList<E> getList(Database db, String name, BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        if (cache.containsKey(name))
            try {
                return (DbList<E>) cache.get(name);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Wrong type! Cached DbList with the given name has a different type than requested.", e);
            }
        else return new DbList<>(db, name, elementToString, elementFromString);
    }

    private DbList(@NotNull Database db, @NotNull String name, @NotNull BiFunction<E, DbCollection, String> elementToString, @NotNull BiFunction<String, DbCollection, E> elementFromString) {
        if (cache.containsKey(name)) throw new IllegalArgumentException("A DbList by this name already exists.");
        checkNotNull(db, "database");
        checkNotNull(elementToString, "elementToString");
        checkNotNull(elementFromString, "elementFromString");
        this.db = db;
        this.table = "list_" + name;
        this.name = name;
        preset.setName(table);
        if (db.getType() == Database.RDBMS.SQLite) preset.getColumns().get("id").setTypeString(db.getType() == Database.RDBMS.SQLite ? "INTEGER" : "INT");
        preset.create(db);
        this.elementToString = elementToString;
        this.elementFromString = elementFromString;
        // Not thread-safe so we use a fixed pool.
        executor = Executors.newFixedThreadPool(1, r -> new Thread(r, "Database List Thread - " + db.getName() + ":" + name));
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
        return db.count(table, "val", null);
    }

    @NotNull
    public CompletableFuture<Integer> sizeAsync() {
        return runAsync(this::size);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @NotNull
    public CompletableFuture<Boolean> isEmptyAsync() {
        return runAsync(this::isEmpty);
    }

    @Override
    public boolean contains(Object o) {
        return db.select(table, "val", QueryCondition.equals("val", elementToString.apply((E) o, this)), null, null).size() > 0;
    }

    @NotNull
    public CompletableFuture<Boolean> containsAsync(Object o) {
        return runAsync(() -> contains(o));
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return toArrayList().iterator();
    }

    @NotNull
    public CompletableFuture<Iterator<E>> iteratorAsync() {
        return toArrayListAsync().thenApply(List::iterator);
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    public CompletableFuture<Object[]> toArrayAsync() {
        return toArrayListAsync().thenApply(List::toArray);
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return toArrayList().toArray(a);
    }

    @NotNull
    public <T> CompletableFuture<T[]> toArrayAsync(@NotNull T[] a) {
        return toArrayListAsync().thenApply(l -> l.toArray(a));
    }

    @Override
    public boolean add(E e) {
        return db.insert(table, "val", elementToString.apply(e, this)) == 1;
    }

    public CompletableFuture<Boolean> addAsync(E e) {
        return runAsync(() -> add(e));
    }

    @Override
    public boolean remove(Object o) {
        boolean b = db.delete(table, QueryCondition.equals("val", elementToString.apply((E) o, this))) > 0;
        fixIndexes();
        return b;
    }

    public CompletableFuture<Boolean> removeAsync(Object o) {
        return runAsync(() -> remove(o));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        for (Object element : c)
            condition.or(QueryCondition.equals("val", elementToString.apply((E) element, this)));
        return db.select(table, new String[] {"val"}, condition, QueryOrder.by("id"), null).size() == c.size();
    }

    public CompletableFuture<Boolean> containsAllAsync(@NotNull Collection<?> c) {
        return runAsync(() -> containsAll(c));
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        List<Object[]> values = new ArrayList<>();
        for (E element : c)
            values.add(new Object[] {elementToString.apply(element, this)});
        return db.insert(table, new String[] {"val"}, values) > 0;
    }

    public CompletableFuture<Boolean> addAllAsync(@NotNull Collection<? extends E> c) {
        return runAsync(() -> addAll(c));
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        List<E> list = toArrayList();
        boolean b = list.addAll(index, c);
        if (b) {
            clear();
            addAll(list);
        }
        return b;
    }

    public CompletableFuture<Boolean> addAllAsync(int index, @NotNull Collection<? extends E> c) {
        return runAsync(() -> addAll(index, c));
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        QueryConditions condition = QueryConditions.create();
        for (Object o : c)
            condition.or(QueryCondition.equals("val", elementToString.apply((E) o, this)));
        int i = db.delete(table, condition);
        fixIndexes();
        return i > 0;
    }

    public CompletableFuture<Boolean> removeAllAsync(@NotNull Collection<?> c) {
        return runAsync(() -> removeAll(c));
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        List<E> list = toArrayList();
        boolean b = list.retainAll(c);
        clear();
        addAll(list);
        return b;
    }

    public CompletableFuture<Boolean> retainAllAsync(@NotNull Collection<?> c) {
        return runAsync(() -> retainAll(c));
    }

    @Override
    public void clear() {
        db.truncate(table);
        if (db.getType() == Database.RDBMS.SQLite) db.delete("sqlite_sequence", QueryCondition.equals("name", table));
    }

    public CompletableFuture<Void> clearAsync() {
        return runAsync(this::clear);
    }

    @Override
    public E get(int index) {
        SelectResults data = db.select(table, "val", QueryCondition.equals("id", index+1), QueryOrder.by("id"), null);
        if (data.isEmpty()) throw exception(index, size());
        else return elementFromString.apply(String.valueOf(data.get(0).get("val")), this);
    }

    public CompletableFuture<E> getAsync(int index) {
        return runAsync(() -> get(index));
    }

    @Override
    public E set(int index, E element) {
        checkIndex(index, size());
        E val = get(index);
        db.insertUpdate(table, new String[] {"id", "val"}, new Object[] {index+1, elementToString.apply(element, this)}, Database.singletonMap("val", elementToString.apply(element, this)), "id");
        return val;
    }

    public CompletableFuture<E> setAsync(int index, E element) {
        return runAsync(() -> set(index, element));
    }

    @Override
    public void add(int index, E element) {
        List<E> list = toArrayList();
        list.add(index, element);
        clear();
        addAll(list);
    }

    public CompletableFuture<Void> addAsync(int index, E element) {
        return runAsync(() -> add(index, element));
    }

    @Override
    public E remove(int index) {
        checkIndex(index, size());
        E element = get(index);
        db.delete(table, QueryCondition.equals("id", index+1));
        fixIndexes();
        return element;
    }

    public CompletableFuture<E> removeAsync(int index) {
        return runAsync(() -> remove(index));
    }

    @Override
    public int indexOf(Object o) {
        SelectResults data = db.select(table, "id", QueryCondition.equals("val", elementToString.apply((E) o, this)), QueryOrder.by("id"), null);
        return data.isEmpty() ? -1 : (Integer) data.get(0).get("id") - 1;
    }

    public CompletableFuture<Integer> indexOfAsync(Object o) {
        return runAsync(() -> indexOf(o));
    }

    @Override
    public int lastIndexOf(Object o) {
        SelectResults data = db.select(table, "id", QueryCondition.equals("val", elementToString.apply((E) o, this)), QueryOrder.by("id"), null);
        return data.isEmpty() ? -1 : (Integer) data.get(data.size()-1).get("id") - 1;
    }

    public CompletableFuture<Integer> lastIndexOfAsync(Object o) {
        return runAsync(() -> lastIndexOf(o));
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return toArrayList().listIterator();
    }

    @NotNull
    public CompletableFuture<ListIterator<E>> listIteratorAsync() {
        return toArrayListAsync().thenApply(List::listIterator);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return toArrayList().listIterator(index);
    }

    @NotNull
    public CompletableFuture<ListIterator<E>> listIteratorAsync(int index) {
        return toArrayListAsync().thenApply(l -> l.listIterator(index));
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        int size = size();
        checkIndex(fromIndex, size);
        checkIndex(toIndex, size);
        List<E> list = new ArrayList<>();
        for (int i = 0; i < toIndex; i++)
            list.add(get(i));
        return list;
    }

    @NotNull
    public CompletableFuture<List<E>> subListAsync(int fromIndex, int toIndex) {
        return runAsync(() -> subList(fromIndex, toIndex));
    }

    @Override
    public String toString() {
        return "DbList[name='" + getName() + "',values=" + super.toString() + "]";
    }

    @NotNull
    public Database getDb() {
        return db;
    }

    @NotNull
    @Override
    public String getTable() {
        return table;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public List<E> toArrayList() {
        return db.select(table, "val", null, QueryOrder.by("id"), null).stream().map(map -> elementFromString.apply(String.valueOf(map.get("val")), this)).collect(Collectors.toList());
    }

    public CompletableFuture<List<E>> toArrayListAsync() {
        return runAsync(this::toArrayList);
    }

    private void fixIndexes() {
        List<E> elements = toArrayList();
        clear();
        addAll(elements);
    }

    private static void checkIndex(int index, int size) {
        if (index >= size) throw exception(index, size);
    }

    private static IndexOutOfBoundsException exception(int index, int size) {
        return new IndexOutOfBoundsException("Index: "+index+", Size: "+size);
    }

    public BiFunction<String, DbCollection, E> getElementFromString() {
        return elementFromString;
    }

    public BiFunction<E, DbCollection, String> getElementToString() {
        return elementToString;
    }
}
