package com.ptsmods.mysqlw.collection;

import com.ptsmods.mysqlw.Database;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Database Collection Functions<br>
 * Functions used to convert instances of various classes to and from Strings.
 */
public class DbCF {
    // Basic type converters
    // Self-explanatory, really.
    public static final BiFunction<String,  DbCollection, String>   strFunc         = (s, c) -> s;
    public static final BiFunction<String,  DbCollection, Byte>     toByteFunc      = (s, c) -> Byte.parseByte(s);
    public static final BiFunction<Byte,    DbCollection, String>   fromByteFunc    = (b, c) -> String.valueOf(b);
    public static final BiFunction<String,  DbCollection, Short>    toShortFunc     = (s, c) -> Short.parseShort(s);
    public static final BiFunction<Short,   DbCollection, String>   fromShortFunc   = (s, c) -> String.valueOf(s);
    public static final BiFunction<String,  DbCollection, Integer>  toIntFunc       = (s, c) -> Integer.parseInt(s);
    public static final BiFunction<Integer, DbCollection, String>   fromIntFunc     = (i, c) -> String.valueOf(i);
    public static final BiFunction<String,  DbCollection, Long>     toLongFunc      = (s, c) -> Long.parseLong(s);
    public static final BiFunction<Long,    DbCollection, String>   fromLongFunc    = (l, c) -> String.valueOf(l);
    public static final BiFunction<String,  DbCollection, Float>    toFloatFunc     = (s, c) -> Float.parseFloat(s);
    public static final BiFunction<Float,   DbCollection, String>   fromFloatFunc   = (f, c) -> String.valueOf(f);
    public static final BiFunction<String,  DbCollection, Double>   toDoubleFunc    = (s, c) -> Double.parseDouble(s);
    public static final BiFunction<Double,  DbCollection, String>   fromDoubleFunc  = (d, c) -> String.valueOf(d);
    private static final Map<Class<?>, Pair<BiFunction<?, DbCollection, String>, BiFunction<String, DbCollection, ?>>> converters = new HashMap<>();

    static {
        registerConverters(String.class,    strFunc,        strFunc);
        registerConverters(Byte.class,      fromByteFunc,   toByteFunc);
        registerConverters(Short.class,     fromShortFunc,  toShortFunc);
        registerConverters(Integer.class,   fromIntFunc,    toIntFunc);
        registerConverters(Long.class,      fromLongFunc,   toLongFunc);
        registerConverters(Float.class,     fromFloatFunc,  toFloatFunc);
        registerConverters(Double.class,    fromDoubleFunc, toDoubleFunc);
    }

    // DbCollection type converters
    /**
     * @param <E> The type of the elements in this list.
     * @return A bifunction to convert a DbList into a String which can be read by {@link #dbListFromStringFunc(BiFunction, BiFunction)} and {@link #dbListFromStringFunc(Class)}.
     * @see #dbListFromStringFunc(BiFunction, BiFunction)
     * @see #dbListFromStringFunc(Class)
     */
    public static <E> BiFunction<DbList<E>, DbCollection, String> dbListToStringFunc() {
        return (l, c) -> "DbList[name=" + Database.enquote(l.getName()) + "]";
    }

    /**
     * @param type The class of the elements in this list. Used to get the type converters registered with {@link #registerConverters(Class, BiFunction, BiFunction)}.
     * @param <E> The type of the elements in this list.
     * @return A bifunction to read the String representation of a DbList produced by {@link #dbListToStringFunc()}.
     * @see #dbListToStringFunc()
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <E> BiFunction<String, DbCollection, DbList<E>> dbListFromStringFunc(Class<E> type) {
        return dbListFromStringFunc(getTo(type), getFrom(type));
    }

    /**
     * @param elementToString The bifunction to convert the elements in this list to Strings.
     * @param elementFromString The bifunction to convert Strings to elements fit for this list.
     * @param <E> The type of the elements in this list.
     * @return A bifunction to read the String representation of a DbList produced by {@link #dbListToStringFunc()}.
     * @see #dbListToStringFunc()
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <E> BiFunction<String, DbCollection, DbList<E>> dbListFromStringFunc(BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        return (s, c) -> DbList.parseString(c.getDb(), s, elementToString, elementFromString);
    }

    /**
     * @param <E> The type of the elements in this set.
     * @return A bifunction to convert a DbSet into a String which can be read by {@link #dbSetFromStringFunc(BiFunction, BiFunction)} and {@link #dbSetFromStringFunc(Class)}.
     * @see #dbSetFromStringFunc(BiFunction, BiFunction)
     * @see #dbSetFromStringFunc(Class)
     */
    public static <E> BiFunction<DbSet<E>, DbCollection, String> dbSetToStringFunc() {
        return (s, c) -> "DbSet[name=" + Database.enquote(s.getName()) + "]";
    }

    /**
     * @param type The class of the elements in this set. Used to get the type converters registered with {@link #registerConverters(Class, BiFunction, BiFunction)}.
     * @param <E> The type of the elements in this set.
     * @return A bifunction to read the String representation of a DbSet produced by {@link #dbSetToStringFunc()}.
     * @see #dbSetToStringFunc()
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <E> BiFunction<String, DbCollection, DbSet<E>> dbSetFromStringFunc(Class<E> type) {
        return dbSetFromStringFunc(getTo(type), getFrom(type));
    }

    /**
     * @param elementToString The bifunction to convert the elements in this list to Strings.
     * @param elementFromString The bifunction to convert Strings to elements fit for this list.
     * @param <E> The type of the elements in this list.
     * @return A bifunction to read the String representation of a DbList produced by {@link #dbListToStringFunc()}.
     * @see #dbListToStringFunc()
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <E> BiFunction<String, DbCollection, DbSet<E>> dbSetFromStringFunc(BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        return (s, c) -> DbSet.parseString(c.getDb(), s, elementToString, elementFromString);
    }

    /**
     * @param <K> The type of the keys in this map.
     * @param <V> The type of the values in this map.
     * @return A bifunction to convert a DbMap into a String which can be read by {@link #dbMapFromStringFunc(BiFunction, BiFunction, BiFunction, BiFunction)} and {@link #dbMapFromStringFunc(Class, Class)}.
     * @see #dbMapFromStringFunc(BiFunction, BiFunction, BiFunction, BiFunction)
     * @see #dbMapFromStringFunc(Class, Class)
     */
    public static <K, V> BiFunction<DbMap<K, V>, DbCollection, String> dbMapToStringFunc() {
        return (child, parent) -> "DbMap[name=" + Database.enquote(child.getName()) + "]";
    }

    /**
     * @param keyType The class of the keys in this map. Used to get the type converters registered with {@link #registerConverters(Class, BiFunction, BiFunction)}.
     * @param valueType The class of the values in this map. Used to get the type converters registered with {@link #registerConverters(Class, BiFunction, BiFunction)}.
     * @param <K> The type of the keys in this map.
     * @param <V> The type of the values in this map.
     * @return A bifunction to read the String representation of a DbMap produced by {@link #dbMapToStringFunc()}.
     * @see #dbMapToStringFunc()
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <K, V> BiFunction<String, DbCollection, DbMap<K, V>> dbMapFromStringFunc(Class<K> keyType, Class<V> valueType) {
        return dbMapFromStringFunc(getTo(keyType), getTo(valueType), getFrom(keyType), getFrom(valueType));
    }

    /**
     *
     * @param keyToString The bifunction to convert the keys in this map to Strings.
     * @param valueToString The bifunction to convert the values in this map to Strings.
     * @param keyFromString The bifunction to convert Strings to keys fit for this map.
     * @param valueFromString The bifunction to convert Strings to values fit for this map.
     * @param <K> The type of the keys in this map.
     * @param <V> The type of the values in this map.
     * @return A bifunction to read the String representation of a DbMap produced by {@link #dbMapToStringFunc()}.
     * @see #dbMapToStringFunc()
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <K, V> BiFunction<String, DbCollection, DbMap<K, V>> dbMapFromStringFunc(BiFunction<K, DbCollection, String> keyToString, BiFunction<V, DbCollection, String> valueToString, BiFunction<String, DbCollection, K> keyFromString, BiFunction<String, DbCollection, V> valueFromString) {
        return (s, c) -> DbMap.parseString(c.getDb(), s, keyToString, valueToString, keyFromString, valueFromString);
    }

    // Custom type converters
    /**
     * Used to register a bifunction to convert Objects of type T to Strings and a bifunction to convert Strings to Objects of type T.
     * @param type The class of type T
     * @param toString The bifunction to convert Objects of type T to Strings.
     * @param fromString The bifunction to convert Strings to objects of type T.
     * @param <T> The type these functions can convert to and from.
     * @see #getTo(Class)
     * @see #getFrom(Class)
     */
    public static <T> void registerConverters(Class<T> type, BiFunction<T, DbCollection, String> toString, BiFunction<String, DbCollection, T> fromString) {
        converters.put(type, new Pair<>(toString, fromString));
    }

    /**
     * @param type The class of type T.
     * @param <T> The type of Objects you wish to convert.
     * @return The bifunction to convert Objects of type T to Strings, registered with {@link #registerConverters(Class, BiFunction, BiFunction)}.
     * @see #getFrom(Class)
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <T> BiFunction<T, DbCollection, String> getTo(Class<T> type) {
        return get(type).getKey();
    }

    /**
     * @param type The class of type T.
     * @param <T> The type of Objects you wish to convert to.
     * @return The bifunction to convert Strings to Objects of type T, registered with {@link #registerConverters(Class, BiFunction, BiFunction)}.
     * @see #getTo(Class)
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <T> BiFunction<String, DbCollection, T> getFrom(Class<T> type) {
        return get(type).getValue();
    }

    /**
     * @param type The class of type T.
     * @param <T> The type of Objects you wish to convert to and from.
     * @return A pair of both {@link #getTo(Class)} and {@link #getFrom(Class)}.
     * @see #getTo(Class)
     * @see #getFrom(Class)
     * @see #registerConverters(Class, BiFunction, BiFunction)
     */
    public static <T> Pair<BiFunction<T, DbCollection, String>, BiFunction<String, DbCollection, T>> get(Class<T> type) {
        if (!converters.containsKey(type)) return new Pair<>(null, null);
        Pair<BiFunction<?, DbCollection, String>, BiFunction<String, DbCollection, ?>> pair = converters.get(type);
        return new Pair<>((BiFunction<T, DbCollection, String>) pair.getKey(), (BiFunction<String, DbCollection, T>) pair.getValue());
    }
}
