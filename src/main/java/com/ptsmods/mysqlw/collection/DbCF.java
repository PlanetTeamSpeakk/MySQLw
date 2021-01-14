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

    // DbCollection type converters
    public static <E> BiFunction<DbList<E>, DbCollection, String> dbListToStringFunc() {
        return (l, m) -> "DbList[name=" + Database.enquote(l.getName()) + "]";
    }

    public static <E> BiFunction<String, DbCollection, DbList<E>> dbListFromStringFunc(BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        return (s, m) -> DbList.parseString(m.getDb(), s, elementToString, elementFromString);
    }

    public static <E> BiFunction<DbSet<E>, DbCollection, String> dbSetToStringFunc() {
        return (s, m) -> "DbSet[name=" + Database.enquote(s.getName()) + "]";
    }

    public static <E> BiFunction<String, DbCollection, DbSet<E>> dbSetFromStringFunc(BiFunction<E, DbCollection, String> elementToString, BiFunction<String, DbCollection, E> elementFromString) {
        return (s, m) -> DbSet.parseString(m.getDb(), s, elementToString, elementFromString);
    }

    public static <K, V> BiFunction<DbMap<K, V>, DbCollection, String> dbMapToStringFunc() {
        return (child, parent) -> "DbMap[name=" + Database.enquote(child.getName()) + "]";
    }

    public static <K, V> BiFunction<String, DbCollection, DbMap<K, V>> dbMapFromStringFunc(BiFunction<K, DbCollection, String> keyToString, BiFunction<V, DbCollection, String> valueToString, BiFunction<String, DbCollection, K> keyFromString, BiFunction<String, DbCollection, V> valueFromString) {
        return (s, m) -> DbMap.parseString(m.getDb(), s, keyToString, valueToString, keyFromString, valueFromString);
    }

    // Custom type converters
    public static <T> void registerConverters(Class<T> type, BiFunction<T, DbCollection, String> toString, BiFunction<String, DbCollection, T> fromString) {
        converters.put(type, new Pair<>(toString, fromString));
    }

    public static <T> BiFunction<T, DbCollection, String> getTo(Class<T> type) {
        return get(type).getKey();
    }

    public static <T> BiFunction<String, DbCollection, T> getFrom(Class<T> type) {
        return get(type).getValue();
    }

    public static <T> Pair<BiFunction<T, DbCollection, String>, BiFunction<String, DbCollection, T>> get(Class<T> type) {
        Pair<BiFunction<?, DbCollection, String>, BiFunction<String, DbCollection, ?>> pair = converters.get(type);
        return new Pair<>((BiFunction<T, DbCollection, String>) pair.getKey(), (BiFunction<String, DbCollection, T>) pair.getValue());
    }
}
