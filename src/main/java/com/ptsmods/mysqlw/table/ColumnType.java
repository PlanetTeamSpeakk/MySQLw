package com.ptsmods.mysqlw.table;

import com.google.common.collect.ImmutableMap;
import com.ptsmods.mysqlw.Database;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ColumnType<S> {

    // NUMERIC TYPES
    /**
     * Tiny integer type (byte)<br>
     * <b>SIGNED:</b> -128 to 127<br>
     * <b>UNSIGNED:</b> 0 to 255
     */
    public static final ColumnType<Function<Integer, String>> TINYINT = new ColumnType<>(length -> parseLengthType("TINYINT", length));
    /**
     * Small integer type (short)<br>
     * <b>SIGNED:</b> -32,768 to 32,767<br>
     * <b>UNSIGNED:</b> 0 to 65,535
     */
    public static final ColumnType<Function<Integer, String>> SMALLINT = new ColumnType<>(length -> parseLengthType("SMALLINT", length));
    /**
     * Medium integer type (no Java equivalent, 24-bit integer)<br>
     * <b>SIGNED:</b> -8,388,608 to 8,388,607<br>
     * <b>UNSIGNED:</b> 0 to 16,777,215
     */
    public static final ColumnType<Function<Integer, String>> MEDIUMINT = new ColumnType<>(length -> parseLengthType("MEDIUMINT", length));
    /**
     * Integer type (int)<br>
     * <b>SIGNED:</b> -2,147,483,648 to 2,147,483,647<br>
     * <b>UNSIGNED:</b> 0 to 4,294,967,295
     */
    public static final ColumnType<Function<Integer, String>> INT = new ColumnType<>(length -> parseLengthType("INT", length));
    /**
     * Big integer type (long)<br>
     * <b>SIGNED:</b> -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807<br>
     * <b>UNSIGNED:</b> 0 to 18,446,744,073,709,551,615
     */
    public static final ColumnType<Function<Integer, String>> BIGINT = new ColumnType<>(length -> parseLengthType("BIGINT", length));

    /**
     * Small decimal type (float)<br>
     * Accurate up to around 7 decimal places.<br>
     * Allowed values: -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38
     */
    public static final ColumnType<BiFunction<Integer, Integer, String>> FLOAT = new ColumnType<>((length, precision) -> parseDecimal("FLOAT", length, precision));
    /**
     * Regular decimal type (double)<br>
     * Accurate up to around 16 decimal places.<br>
     * Allowed values: -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38
     */
    public static final ColumnType<BiFunction<Integer, Integer, String>> DOUBLE = new ColumnType<>((length, precision) -> parseDecimal("DOUBLE", length, precision));
    /**
     * Big decimal type (BigDecimal)<br>
     * <b>Fixed point</b><br>
     * Accurate up to up to 30 decimal places and up to 65 digits.<br>
     * By default, accurate up to 0 decimals and 10 digits.<br>
     * Allowed values: -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38
     */
    public static final ColumnType<BiFunction<Integer, Integer, String>> DECIMAL = new ColumnType<>((length, precision) -> parseDecimal("DECIMAL", length, precision));
    /**
     * Synonym for {@link #DOUBLE} unless the SQL option REAL_AS_FLOAT is set in which case it's a synonym for {@link #FLOAT}.
     * @see #DOUBLE
     * @see #FLOAT
     */
    public static final ColumnType<BiFunction<Integer, Integer, String>> REAL = new ColumnType<>((length, precision) -> parseDecimal("REAL", length, precision));


    // DATE AND TIME TYPES
    /**
     * Date type<br>
     * A date that supports a value anywhere between 1000-01-01 and 9999-12-31.<br>
     * Format of its String representation is yyyy-mm-dd.
     */
    public static final ColumnType<Supplier<String>> DATE = new ColumnType<>(() -> "DATE");
    /**
     * Datetime type<br>
     * A combination of {@link #DATE} and {@link #TIME}.
     * @deprecated Java by default has classes for {@link java.sql.Date Date}, {@link java.sql.Timestamp Timestamp} and {@link java.sql.Time Time}, but not for {@code datetime}. So it is recommended to use any of those, especially {@link #TIMESTAMP}, instead.
     */
    @Deprecated
    public static final ColumnType<Supplier<String>> DATETIME = new ColumnType<>(() -> "DATETIME");
    /**
     * Timestamp type<br>
     * Stored as an integer of seconds since epoch.<br>
     * Contains a date and time.<br>
     * Supported range is 1970-01-01 00:00:01 UTC to 2038-01-09 03:14:07 UTC.
     */
    public static final ColumnType<Supplier<String>> TIMESTAMP = new ColumnType<>(() -> "TIMESTAMP");
    /**
     * Time type<br>
     * Holds just a time in the format hh:mm:ss.<br>
     * Supported range is -838:59:59 to 838:59:59.
     */
    public static final ColumnType<Supplier<String>> TIME = new ColumnType<>(() -> "TIME");
    /**
     * Year type<br>
     * A year stored with either four (default) or two digits.<br>
     * YEAR(2) is deprecated and no longer supported, so this library does not support it either.
     */
    public static final ColumnType<Supplier<String>> YEAR = new ColumnType<>(() -> "YEAR");


    // STRING TYPES
    /**
     * String with fixed length<br>
     * Length must be at most 255.
     */
    public static final ColumnType<Function<Integer, String>> CHAR = new ColumnType<>(length -> "CHAR(" + length + ")");
    /**
     * String with varying length<br>
     * Max length must be at most 255.
     */
    public static final ColumnType<Function<Integer, String>> VARCHAR = new ColumnType<>(maxlength -> "VARCHAR(" + maxlength + ")");

    /**
     * Tiny text type<br>
     * Can store up to 255 characters.
     */
    public static final ColumnType<Supplier<String>> TINYTEXT = new ColumnType<>(() -> "TINYTEXT");
    /**
     * Text type<br>
     * Can store up to 65,535 characters.
     */
    public static final ColumnType<Supplier<String>> TEXT = new ColumnType<>(() -> "TEXT");
    /**
     * Medium text type<br>
     * Can store up to 16,777,215 characters.
     */
    public static final ColumnType<Supplier<String>> MEDIUMTEXT = new ColumnType<>(() -> "MEDIUMTEXT");
    /**
     * Long text type<br>
     * Can store up to 4,294,967,295 characters.
     */
    public static final ColumnType<Supplier<String>> LONGTEXT = new ColumnType<>(() -> "LONGTEXT");

    /**
     * Same as {@link #CHAR} except Strings are stored as binary byte strings rather than character strings.
     */
    public static final ColumnType<Function<Integer, String>> BINARY = new ColumnType<>(length -> parseLengthType("BINARY", length));
    /**
     * Same as {@link #VARCHAR} except Strings are stored as binary byte strings rather than character strings.
     */
    public static final ColumnType<Function<Integer, String>> VARBINARY = new ColumnType<>(length -> parseLengthType("VARBINARY", length));


    // BLOBS (Binary Large OBject)
    /**
     * Tiny Binary Large OBject<br>
     * <b>Max size:</b> 255 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     */
    public static final ColumnType<Supplier<String>> TINYBLOB = new ColumnType<>(() -> "TINYBLOB");
    /**
     * Binary Large OBject<br>
     * <b>Max size:</b>65,635 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     */
    public static final ColumnType<Supplier<String>> BLOB = new ColumnType<>(() -> "BLOB");
    /**
     * Medium Binary Large OBject<br>
     * <b>Max size:</b> 16,777,215 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     */
    public static final ColumnType<Supplier<String>> MEDIUMBLOB = new ColumnType<>(() -> "MEDIUMBLOB");
    /**
     * Long Binary Large OBject<br>
     * <b>Max size:</b> 4,294,967,295 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     */
    public static final ColumnType<Supplier<String>> LONGBLOB = new ColumnType<>(() -> "LONGBLOB");


    // ENUM AND SET
    /**
     * Enum type<br>
     * Can only hold the values given when the table was created (can be altered), the index corresponding with a value or the error value (empty string or 0).<br>
     * Can hold up to 65,635 different values.
     */
    public static final ColumnType<Function<String[], String>> ENUM = new ColumnType<>(values -> parseSetOrEnum("ENUM", values));
    /**
     * Set type<br>
     * Same as enum, but can only hold up to 64 different values, does not have an error type (so it can hold empty strings) and cannot hold indexes corresponding with a value.<br>
     * So it can only hold the values given when initialised.
     */
    public static final ColumnType<Function<String[], String>> SET = new ColumnType<>(values -> parseSetOrEnum("SET", values));


    // GEOMETRY (basically all just BLOBs)
    // Little to no support for these
    // If you intend to use any of these, it is recommended to make your own class for them and register a type converter using Database#registerTypeConverter.
    // Inb4, apparently the values of these things are OpenGIS classes. Just saying...
    /**
     * Geometry type<br>
     * Can store a geometry of any type.
     */
    public static final ColumnType<Supplier<String>> GEOMETRY = new ColumnType<>(() -> "GEOMETRY");
    /**
     * Point type<br>
     * Holds a coordinate with an X and Y value (and more dimensions of you choose to).
     */
    public static final ColumnType<Supplier<String>> POINT = new ColumnType<>(() -> "POINT");
    /**
     * Linestring type<br>
     * A curve with linear interpolation between points.
     */
    public static final ColumnType<Supplier<String>> LINESTRING = new ColumnType<>(() -> "LINESTRING");
    /**
     * Polygon type<br>
     * You know what a polygon is, right?<br>
     * If not, it's basically any 2D shape with straight lines.
     */
    public static final ColumnType<Supplier<String>> POLYGON = new ColumnType<>(() -> "POLYGON");
    /**
     * Multipoint type<br>
     * A collection of points
     * @see #POINT
     */
    public static final ColumnType<Supplier<String>> MULTIPOINT = new ColumnType<>(() -> "MULTIPOINT");
    /**
     * Multilinestring type<br>
     * A collection of linestrings
     * @see #LINESTRING
     */
    public static final ColumnType<Supplier<String>> MULTILINESTRING = new ColumnType<>(() -> "MULTILINESTRING");
    /**
     * Multipolygon type<br>
     * A collection of polygons
     * @see #POLYGON
     */
    public static final ColumnType<Supplier<String>> MULTIPOLYGON = new ColumnType<>(() -> "MULTIPOLYGON");
    /**
     * Geometrycollection type<br>
     * A collection of geometry objects of any type
     * @see #GEOMETRY
     */
    public static final ColumnType<Supplier<String>> GEOMETRYCOLLECTION = new ColumnType<>(() -> "GEOMETRYCOLLECTION");


    // JSON
    /**
     * JSON type<br>
     * Just a string, but it's supposed to resemble JSON data and you can do cool tricks when selecting using JSON_CONTAINS.
     */
    public static final ColumnType<Supplier<String>> JSON = new ColumnType<>(() -> "JSON");

    public static final Map<String, ColumnType<?>> types;

    static {
        Map<String, ColumnType<?>> types0 = new HashMap<>();
        for (Field f : ColumnType.class.getFields())
            if (Modifier.isPublic(f.getModifiers()) && f.getType() == ColumnType.class) {
                try {
                    types0.put(f.getName().toUpperCase(Locale.ROOT), (ColumnType<?>) f.get(null));
                } catch (IllegalAccessException ignored) {}
            }
        types = ImmutableMap.copyOf(types0);
    }

    private final S supplier;

    private ColumnType(S supplier) {
        this.supplier = supplier;
    }

    public S getSupplier() {
        return supplier;
    }

    public ColumnStructure<S> createStructure() {
        return new ColumnStructure<>(this);
    }

    private static String parseLengthType(String type, Integer length) {
        return length == null ? type : type + "(" + length + ")";
    }

    private static String parseDecimal(String type, Integer length, Integer precision) {
        if (length == null && precision != null) throw new IllegalArgumentException("Cannot set precision without setting length");
        return length == null ? type : type + "(" + length + (precision == null ? "" : "," + precision) + ")";
    }

    private static String parseSetOrEnum(String type, String[] values) {
        for (int i = 0; i < values.length; i++)
            values[i] = Database.enquote(values[i]);
        return type + "(" + String.join(",", values) + ")";
    }

}
