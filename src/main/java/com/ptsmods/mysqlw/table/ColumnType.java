package com.ptsmods.mysqlw.table;

import com.ptsmods.mysqlw.Database;
import com.ptsmods.mysqlw.Pair;
import com.ptsmods.mysqlw.table.configurable.Dynamic2Configurable;
import com.ptsmods.mysqlw.table.configurable.DynamicConfigurable;
import com.ptsmods.mysqlw.table.configurable.DynamicNConfigurable;
import com.ptsmods.mysqlw.table.configurable.SimpleConfigurable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ColumnType<S> {

    private static final Map<String, ColumnType<?>> typesBackend = new LinkedHashMap<>();
    // NUMERIC TYPES
    /**
     * Boolean type (tinyint)<br>
     * Basically the same as a {@link #TINYINT}, but with a length of 1.<br>
     * Gets treated as a boolean by MySQL and the MySQL connector. (Possibly also SQLite, but this is untested)
     */
    public static final ColumnType<SimpleConfigurable> BOOLEAN = new ColumnType<>("BOOLEAN", type ->
            () -> type == Database.RDBMS.SQLite ? "BOOLEAN" : parseLengthType("TINYINT", 1), true);
    /**
     * Tiny integer type (byte)<br>
     * <b>SIGNED:</b> -128 to 127<br>
     * <b>UNSIGNED:</b> 0 to 255
     */
    public static final ColumnType<DynamicConfigurable<Integer>> TINYINT = new ColumnType<>("TINYINT",
            ((DynamicConfigurable<Integer>) (length -> parseLengthType("TINYINT", length))).withDefault(null));
    /**
     * Small integer type (short)<br>
     * <b>SIGNED:</b> -32,768 to 32,767<br>
     * <b>UNSIGNED:</b> 0 to 65,535
     */
    public static final ColumnType<DynamicConfigurable<Integer>> SMALLINT = new ColumnType<>("SMALLINT",
            ((DynamicConfigurable<Integer>) (length -> parseLengthType("SMALLINT", length))).withDefault(null));
    /**
     * Medium integer type (no Java equivalent, 24-bit integer)<br>
     * <b>SIGNED:</b> -8,388,608 to 8,388,607<br>
     * <b>UNSIGNED:</b> 0 to 16,777,215
     */
    public static final ColumnType<DynamicConfigurable<Integer>> MEDIUMINT = new ColumnType<>("MEDIUMINT",
            ((DynamicConfigurable<Integer>) (length -> parseLengthType("MEDIUMINT", length))).withDefault(null));
    /**
     * Integer type (int)<br>
     * <b>SIGNED:</b> -2,147,483,648 to 2,147,483,647<br>
     * <b>UNSIGNED:</b> 0 to 4,294,967,295
     */
    public static final ColumnType<Function<Integer, String>> INT = new ColumnType<>("INT",
            type -> ((DynamicConfigurable<Integer>) (length -> parseLengthType(type == Database.RDBMS.SQLite ?
                    "INTEGER" : "INT", length))).withDefault(null), true); // To support primary keys
            // INTEGER and INT are not the *exact* same, but you must use INTEGER to use primary keys in SQLite.
            // (https://stackoverflow.com/a/7337945)
    /**
     * Big integer type (long)<br>
     * <b>SIGNED:</b> -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807<br>
     * <b>UNSIGNED:</b> 0 to 18,446,744,073,709,551,615
     */
    public static final ColumnType<DynamicConfigurable<Integer>> BIGINT = new ColumnType<>("BIGINT",
            ((DynamicConfigurable<Integer>) (length -> parseLengthType("BIGINT", length))).withDefault(null));

    /**
     * Small decimal type (float)<br>
     * Accurate up to around 7 decimal places.<br>
     * Allowed values: -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38
     */
    public static final ColumnType<Dynamic2Configurable<Integer, Integer>> FLOAT = new ColumnType<>("FLOAT",
            ((Dynamic2Configurable<Integer, Integer>) ((length, precision) -> parseDecimal("FLOAT", length, precision)))
                    .withDefault(Pair.empty()));
    /**
     * Regular decimal type (double)<br>
     * Accurate up to around 16 decimal places.<br>
     * Allowed values: -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38
     */
    public static final ColumnType<Dynamic2Configurable<Integer, Integer>> DOUBLE = new ColumnType<>("DOUBLE",
            ((Dynamic2Configurable<Integer, Integer>) ((length, precision) -> parseDecimal("DOUBLE", length, precision)))
                    .withDefault(Pair.empty()));
    /**
     * Big decimal type (BigDecimal)<br>
     * <b>Fixed point</b><br>
     * Accurate up to 30 decimal places and up to 65 digits.<br>
     * By default, accurate up to 0 decimals and 10 digits.<br>
     * Allowed values: -3.402823466E+38 to -1.175494351E-38, 0, and 1.175494351E-38 to 3.402823466E+38
     */
    public static final ColumnType<Dynamic2Configurable<Integer, Integer>> DECIMAL = new ColumnType<>("DECIMAL",
            ((Dynamic2Configurable<Integer, Integer>) ((length, precision) -> parseDecimal("DECIMAL", length, precision)))
                    .withDefault(Pair.empty()));
    /**
     * Synonym for {@link #DOUBLE} unless the SQL option REAL_AS_FLOAT is set in which case it's a synonym for {@link #FLOAT}.
     * @see #DOUBLE
     * @see #FLOAT
     */
    public static final ColumnType<Dynamic2Configurable<Integer, Integer>> REAL = new ColumnType<>("REAL",
            ((Dynamic2Configurable<Integer, Integer>) ((length, precision) -> parseDecimal("REAL", length, precision)))
                    .withDefault(Pair.empty()));


    // DATE AND TIME TYPES
    /**
     * Date type<br>
     * A date that supports a value anywhere between 1000-01-01 and 9999-12-31.<br>
     * Format of its String representation is yyyy-mm-dd.
     */
    public static final ColumnType<SimpleConfigurable> DATE = new ColumnType<>("DATE", () -> "DATE");
    /**
     * Datetime type<br>
     * A combination of {@link #DATE} and {@link #TIME}.
     */
    public static final ColumnType<SimpleConfigurable> DATETIME = new ColumnType<>("DATETIME", () -> "DATETIME");
    /**
     * Timestamp type<br>
     * Stored as an integer of seconds since epoch.<br>
     * Contains a date and time.<br>
     * Supported range is 1970-01-01 00:00:01 UTC to 2038-01-09 03:14:07 UTC.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> TIMESTAMP = new ColumnType<>("TIMESTAMP", () -> "TIMESTAMP");
    /**
     * Time type<br>
     * Holds just a time in the format hh:mm:ss.<br>
     * Supported range is -838:59:59 to 838:59:59.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> TIME = new ColumnType<>("TIME", () -> "TIME");
    /**
     * Year type<br>
     * A year stored with either four (default) or two digits.<br>
     * YEAR(2) is deprecated and no longer supported, so this library does not support it either.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> YEAR = new ColumnType<>("YEAR", () -> "YEAR");


    // STRING TYPES
    /**
     * String with fixed length<br>
     * Length must be at most 255.
     */
    public static final ColumnType<DynamicConfigurable<Integer>> CHAR = new ColumnType<>("CHAR", length -> "CHAR(" + length + ")");
    /**
     * String with varying length<br>
     * Max length must be at most 65535 when using MySQL.
     */
    public static final ColumnType<DynamicConfigurable<Integer>> VARCHAR = new ColumnType<>("VARCHAR", maxlength -> "VARCHAR(" + maxlength + ")");

    /**
     * Tiny text type<br>
     * Can store up to 255 characters.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> TINYTEXT = new ColumnType<>("TINYTEXT", () -> "TINYTEXT");
    /**
     * Text type<br>
     * Can store up to 65,535 characters.
     */
    public static final ColumnType<SimpleConfigurable> TEXT = new ColumnType<>("TEXT", () -> "TEXT");
    /**
     * Medium text type<br>
     * Can store up to 16,777,215 characters.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> MEDIUMTEXT = new ColumnType<>("MEDIUMTEXT", () -> "MEDIUMTEXT");
    /**
     * Long text type<br>
     * Can store up to 4,294,967,295 characters.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> LONGTEXT = new ColumnType<>("LONGTEXT", () -> "LONGTEXT");

    /**
     * Same as {@link #CHAR} except Strings are stored as binary byte strings rather than character strings.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<DynamicConfigurable<Integer>> BINARY = new ColumnType<>("BINARY",
            length -> parseLengthType("BINARY", length));
    /**
     * Same as {@link #VARCHAR} except Strings are stored as binary byte strings rather than character strings.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<DynamicConfigurable<Integer>> VARBINARY = new ColumnType<>("VARBINARY",
            length -> parseLengthType("VARBINARY", length));
    /**
     * Char text type with fixed length of 36 characters, used to store {@link java.util.UUID UUID}s.
     */
    public static final ColumnType<SimpleConfigurable> UUID = new ColumnType<>("UUID", () -> "CHAR(36)");


    // BLOBS (Binary Large OBject)
    /**
     * Tiny Binary Large OBject<br>
     * <b>Max size:</b> 255 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> TINYBLOB = new ColumnType<>("TINYBLOB", () -> "TINYBLOB");
    /**
     * Binary Large OBject<br>
     * <b>Max size:</b>65,635 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     */
    public static final ColumnType<SimpleConfigurable> BLOB = new ColumnType<>("BLOB", () -> "BLOB");
    /**
     * Medium Binary Large OBject<br>
     * <b>Max size:</b> 16,777,215 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> MEDIUMBLOB = new ColumnType<>("MEDIUMBLOB", () -> "MEDIUMBLOB");
    /**
     * Long Binary Large OBject<br>
     * <b>Max size:</b> 4,294,967,295 bytes<br>
     * Used for storing various types of files, s.a. pictures and audio or even video.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> LONGBLOB = new ColumnType<>("LONGBLOB", () -> "LONGBLOB");


    // ENUM AND SET
    /**
     * Enum type<br>
     * Can only hold the values given when the table was created (can be altered), the index corresponding with a value or the error value (empty string or 0).<br>
     * Can hold up to 65,635 different values.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<DynamicNConfigurable<String>> ENUM = new ColumnType<>("ENUM", values -> parseSetOrEnum("ENUM", values));
    /**
     * Set type<br>
     * Same as enum, but can only hold up to 64 different values, does not have an error type (so it can hold empty strings) and cannot hold indexes corresponding with a value.<br>
     * So it can only hold the values given when initialised.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<DynamicNConfigurable<String>> SET = new ColumnType<>("SET", values -> parseSetOrEnum("SET", values));


    // GEOMETRY (basically all just BLOBs)
    // Little to no support for these
    // If you intend to use any of these, it is recommended to make your own class for them and register a type converter using Database#registerTypeConverter.
    // Inb4, apparently the values of these things are OpenGIS classes. Just saying...
    /**
     * Geometry type<br>
     * Can store a geometry of any type.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> GEOMETRY = new ColumnType<>("GEOMETRY", () -> "GEOMETRY");
    /**
     * Point type<br>
     * Holds a coordinate with an X and Y value (and more dimensions of you choose to).
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> POINT = new ColumnType<>("POINT", () -> "POINT");
    /**
     * Linestring type<br>
     * A curve with linear interpolation between points.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> LINESTRING = new ColumnType<>("LINESTRING", () -> "LINESTRING");
    /**
     * Polygon type<br>
     * You know what a polygon is, right?<br>
     * If not, it's basically any 2D shape with straight lines.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> POLYGON = new ColumnType<>("POLYGON", () -> "POLYGON");
    /**
     * Multipoint type<br>
     * A collection of points
     * <br><b>This type is not supported by SQLite</b>
     * @see #POINT
     */
    public static final ColumnType<SimpleConfigurable> MULTIPOINT = new ColumnType<>("MULTIPOINT", () -> "MULTIPOINT");
    /**
     * Multilinestring type<br>
     * A collection of linestrings
     * @see #LINESTRING
     */
    public static final ColumnType<SimpleConfigurable> MULTILINESTRING = new ColumnType<>("MULTILINESTRING", () -> "MULTILINESTRING");
    /**
     * Multipolygon type<br>
     * A collection of polygons
     * <br><b>This type is not supported by SQLite</b>
     * @see #POLYGON
     */
    public static final ColumnType<SimpleConfigurable> MULTIPOLYGON = new ColumnType<>("MULTIPOLYGON", () -> "MULTIPOLYGON");
    /**
     * Geometrycollection type<br>
     * A collection of geometry objects of any type
     * <br><b>This type is not supported by SQLite</b>
     * @see #GEOMETRY
     */
    public static final ColumnType<SimpleConfigurable> GEOMETRYCOLLECTION = new ColumnType<>("GEOMETRYCOLLECTION", () -> "GEOMETRYCOLLECTION");


    // JSON
    /**
     * JSON type<br>
     * Just a string, but it's supposed to resemble JSON data, and you can do cool tricks when selecting using JSON_CONTAINS.
     * <br><b>This type is not supported by SQLite</b>
     */
    public static final ColumnType<SimpleConfigurable> JSON = new ColumnType<>("JSON", () -> "JSON");

    public static final Map<String, ColumnType<?>> types = Collections.unmodifiableMap(typesBackend);

    private final String name;
    private final Function<Database.RDBMS, S> supplier;

    private ColumnType(String name, S supplier) {
        this(name, type -> supplier, true);
    }

    private ColumnType(String name, Function<Database.RDBMS, S> typeSpecificSupplier, boolean ignoredTypeSpecific) {
        this.name = name;
        this.supplier = typeSpecificSupplier;

        typesBackend.put(name, this);
    }

    public String getName() {
        return name;
    }

    @Deprecated
    public S getSupplier() {
        return supplier.apply(Database.RDBMS.UNKNOWN);
    }

    public Function<Database.RDBMS, S> getTypeSpecificSupplier() {
        return supplier;
    }

    /**
     * @return A new {@link ColumnStructure} for this type
     * @deprecated Use the shorthand {@link #struct()} instead.
     */
    @Deprecated
    public ColumnStructure<S> createStructure() {
        return struct();
    }

    /**
     * @return A new {@link ColumnStructure} for this type
     */
    public ColumnStructure<S> struct() {
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
