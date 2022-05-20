package com.ptsmods.mysqlw.collection;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.SelectResults;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TableIndex;
import com.ptsmods.mysqlw.table.TablePreset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.ptsmods.mysqlw.Database.checkNotNull;

@SuppressWarnings("unused")
public class DbMap<K, V> extends AbstractMap<K, V> implements DbCollection {

    private static final TablePreset preset = TablePreset.create("map_")
            .putColumn("m_key", ColumnType.VARCHAR.createStructure()
                    .configure(sup -> sup.apply(255))
                    .setPrimary(true)
                    .setNullAllowed(false))
            .putColumn("m_val", ColumnType.TEXT.createStructure())
            .addIndex(TableIndex.index("m_key", TableIndex.Type.FULLTEXT));
    private static final Map<String, DbMap<?, ?>> cache = new HashMap<>();
    private final Database db;
    private final String table;
    private final String name;
    private final BiFunction<K, DbCollection, String> keyToString;
    private final BiFunction<V, DbCollection, String> valueToString;
    private final BiFunction<String, DbCollection, K> keyFromString;
    private final BiFunction<String, DbCollection, V> valueFromString;
    private Executor executor;

    /**
     * Parses a String representation of a DbMap into a DbMap.
     * @param db The database this map belongs to. Used when creating a new map.
     * @param s The String to parse.
     * @param keyToString The function used to convert a key object of this map into a String. Used when creating a new map.
     * @param valueToString The function used to convert a value object of this map into a String. Used when creating a new map.
     * @param keyFromString The function used to convert a String into a key object of this map. Used when creating a new map.
     * @param valueFromString The function used to convert a String into a value object of this map. Used when creating a new map.
     * @param <K> The key type of this map.
     * @param <V> The value type of this map.
     * @return A new DbMap or a cached one if available.
     */
    public static <K, V> DbMap<K, V> parseString(Database db, String s, BiFunction<K, DbCollection, String> keyToString, BiFunction<V, DbCollection, String> valueToString, BiFunction<String, DbCollection, K> keyFromString, BiFunction<String, DbCollection, V> valueFromString) {
        return s.startsWith("DbMap[name=") ? getMap(db, Database.readQuotedString(s.substring("DbMap[name=".length())), keyToString, valueToString, keyFromString, valueFromString) : null;
    }

    /**
     * Gets a map from cache or creates a new one.
     * @param db The database this map belongs to. Used when creating a new map.
     * @param name The name of this map.
     * @param keyType The class of the type of the keys in this map, registered at {@link DbCF}. Used when creating a new map.
     * @param valueType The class of the type of the values in this map, registered at {@link DbCF}. Used when creating a new map.
     * @param <K> The type of the keys in this map.
     * @param <V> The type of the values in this map.
     * @return A new DbMap or a cached one if available.
     */
    public static <K, V> DbMap<K, V> getMap(Database db, String name, Class<K> keyType, Class<V> valueType) {
        return getMap(db, name, DbCF.getTo(keyType), DbCF.getTo(valueType), DbCF.getFrom(keyType), DbCF.getFrom(valueType));
    }

    /**
     * Gets a map from cache or creates a new one.
     * @param db The database this map belongs to. Used when creating a new map.
     * @param name The name of this map.
     * @param keyToString The function used to convert a key object of this map into a String. Used when creating a new map.
     * @param valueToString The function used to convert a value object of this map into a String. Used when creating a new map.
     * @param keyFromString The function used to convert a String into a key object of this map. Used when creating a new map.
     * @param valueFromString The function used to convert a String into a value object of this map. Used when creating a new map.
     * @param <K> The type of the keys in this map.
     * @param <V> The type of the values in this map.
     * @return A new DbMap or a cached one if available.
     */
    public static <K, V> DbMap<K, V> getMap(@NotNull Database db, @NotNull String name,
											@NotNull BiFunction<K, DbCollection, String> keyToString, @NotNull BiFunction<V, DbCollection, String> valueToString,
											@NotNull BiFunction<String, DbCollection, K> keyFromString, @NotNull BiFunction<String, DbCollection, V> valueFromString) {
        if (cache.containsKey(name))
            try {
                return (DbMap<K, V>) cache.get(name);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Wrong types! Cached DbMap with the given name has different types than requested.", e);
            }
        else return new DbMap<>(db, name, keyToString, valueToString, keyFromString, valueFromString);
    }

    private DbMap(Database db, String name, BiFunction<K, DbCollection, String> keyToString, BiFunction<V, DbCollection, String> valueToString, BiFunction<String, DbCollection, K> keyFromString, BiFunction<String, DbCollection, V> valueFromString) {
        if (cache.containsKey(name)) throw new IllegalArgumentException("A DbMap by this name already exists.");
        checkNotNull(db, "database");
        checkNotNull(keyToString, "keyToString");
        checkNotNull(keyFromString, "keyFromString");
        checkNotNull(valueToString, "valueToString");
        checkNotNull(valueFromString, "valueFromString");
        this.db = db;
        this.table = "map_" + name;
        this.name = name;
        // We could first check if the table exists, but if we're gonna make a call to the database anyway,
        // we might as well just make one call that only creates a new table if it does not yet exist.
        // Otherwise we'd have to make a call to check if the table exists and then one to make it if it doesn't.
        preset.setName(table).create(db);
        this.keyToString = keyToString;
        this.valueToString = valueToString;
        this.keyFromString = keyFromString;
        this.valueFromString = valueFromString;
        // Not thread-safe so we use a fixed pool.
        executor = Executors.newFixedThreadPool(1, r -> new Thread(r, "Database Map Thread - " + db.getName() + ":" + name));
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
        return db.count(table, "m_key", null);
    }

    public CompletableFuture<Integer> sizeAsync() {
        return runAsync(this::size);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    public CompletableFuture<Boolean> isEmptyAsync() {
        return runAsync(this::isEmpty);
    }

    @Override
    public boolean containsKey(Object key) {
        return db.select(table, "m_key", QueryCondition.equals("m_key", keyToString.apply((K) key, this)), null, null).size() > 0;
    }

    public CompletableFuture<Boolean> containsKeyAsync(Object key) {
        return runAsync(() -> containsKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return db.select(table, "m_val", QueryCondition.equals("m_val", value == null ? null : valueToString.apply((V) value, this)), null, null).size() > 0;
    }

    public CompletableFuture<Boolean> containsValueAsync(Object value) {
        return runAsync(() -> containsValue(value));
    }

    @Override
    public V get(Object key) {
        SelectResults data = db.select(table, "m_val", QueryCondition.equals("m_key", keyToString.apply((K) key, this)), null, null);
        if (data.isEmpty()) return null;
        else return data.get(0).get("m_val") == null ? null : valueFromString.apply(String.valueOf(data.get(0).get("m_val")), this);
    }

    public CompletableFuture<V> getAsync(Object key) {
        return runAsync(() -> get(key));
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        if (key == null) throw new NullPointerException("Key cannot be null.");
        V old = get(key);
        db.replace(table, new String[] {"m_key", "m_val"}, new String[] {keyToString.apply(key, this), value == null ? null : valueToString.apply(value, this)});
        return old;
    }

    @Nullable
    public CompletableFuture<V> putAsync(K key, V value) {
        return runAsync(() -> put(key, value));
    }

    @Override
    public V remove(Object key) {
        V value = get(key);
        db.delete(table, QueryCondition.equals("m_key", keyToString.apply((K) key, this)));
        return value;
    }

    public CompletableFuture<V> removeAsync(Object key) {
        return runAsync(() -> remove(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) { // Way more efficient to put them all in at once than going at it one by one and calling #put.
        List<Object[]> columnValues = new ArrayList<>();
        m.forEach((key, value) -> {if (key != null) columnValues.add(new Object[] {keyToString.apply(key, this), value == null ? null : valueToString.apply(value, this)});});
        db.replace(table, new String[] {"m_key", "m_val"}, columnValues); // We don't need duplicate keys on our hands.
    }

    public CompletableFuture<Void> putAllAsync(@NotNull Map<? extends K, ? extends V> m) {
        return runAsync(() -> putAll(m));
    }

    @Override
    public void clear() {
        db.truncate(table);
    }

    public CompletableFuture<Void> clearAsync() {
        return runAsync(this::clear);
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        Set<K> keySet = new LinkedHashSet<>();
        for (SelectResults.SelectResultRow row : db.select(table, "m_key", null, null, null))
            keySet.add(keyFromString.apply(String.valueOf(row.get("m_key")), this));
        return keySet;
    }

    @NotNull
    public CompletableFuture<Set<K>> keySetAsync() {
        return runAsync(this::keySet);
    }

    @NotNull
    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (SelectResults.SelectResultRow row : db.select(table, "m_val", null, null, null))
            values.add(row.get("m_val") == null ? null : valueFromString.apply(String.valueOf(row.get("m_val")), this));
        return values;
    }

    @NotNull
    public CompletableFuture<Collection<V>> valuesAsync() {
        return runAsync(this::values);
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        SelectResults data = db.select(table, new String[] {"m_key", "m_val"}, null, null, null);
        Set<Entry<K, V>> entrySet = new LinkedHashSet<>();
        for (SelectResults.SelectResultRow row : data)
            entrySet.add(new DbMapEntry<>(this, row));
        return entrySet;
    }

    @NotNull
    public CompletableFuture<Set<Entry<K, V>>> entrySetAsync() {
        return runAsync(this::entrySet);
    }

    @Override
    public String toString() {
        return "DbMap[name='" + name + "',values=" + super.toString() + "]";
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

    public String getName() {
        return name;
    }

    @NotNull
    public BiFunction<K, DbCollection, String> getKeyToString() {
        return keyToString;
    }

    @NotNull
    public BiFunction<V, DbCollection, String> getValueToString() {
        return valueToString;
    }

    @NotNull
    public BiFunction<String, DbCollection, K> getKeyFromString() {
        return keyFromString;
    }

    @NotNull
    public BiFunction<String, DbCollection, V> getValueFromString() {
        return valueFromString;
    }

    public static class DbMapEntry<K, V> implements Entry<K, V> {
        private final DbMap<K, V> map;
        private final SelectResults.SelectResultRow row;

        private DbMapEntry(DbMap<K, V> map, SelectResults.SelectResultRow row) {
            this.map = map;
            this.row = row;
        }

        @Override
        public K getKey() {
            return map.getKeyFromString().apply(row.getString("m_key"), map);
        }

        @Override
        public V getValue() {
            return row.get("m_val") == null ? null : map.getValueFromString().apply(row.getString("m_val"), map);
        }

        @Override
        public V setValue(V value) {
            return map.put(getKey(), value);
        }
    }
}
