package com.ptsmods.mysqlw;

import com.ptsmods.mysqlw.query.QueryCondition;
import com.ptsmods.mysqlw.query.QueryFunction;
import com.ptsmods.mysqlw.query.QueryOrder;
import com.ptsmods.mysqlw.query.SelectResults;
import com.ptsmods.mysqlw.table.TablePreset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    private static final Map<Class<?>, Function<Object, String>> classConverters = new HashMap<>();

    /**
     * Makes a connection to a MySQL database.
     * @param host The hostname of this database. Often localhost
     * @param port The port this dataserver runs on. Often 3306
     * @param name The name of this database. An attempt to create this database will be made if it does not yet exist.
     * @param username The username to log in with.
     * @param password The password that goes with the username. Can be null if there isn't one.
     * @return A Database with which you can do anything.
     * @throws SQLException If an error occurred while either connecting or creating the database.
     */
    public static Database connect(String host, int port, String name, String username, String password) throws SQLException {
        Database db = new Database(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port, username, password), name);
        db.execute("CREATE DATABASE IF NOT EXISTS " + name + ";"); // Create database if it does not yet exist.
        db.getConnection().setCatalog(name);
        return db;
    }

    private final Connection con;
    private final Logger log;
    private final String cachedName;

    private Database(Connection con, String name) {
        this.con = con;
        log = Logger.getLogger("Database-" + name);
        cachedName = name;
    }

    public Logger getLog() {
        return log;
    }

    /**
     * Attempts to get the name of the database currently in use.
     * @return The name of the currently in use database, or the cached name if it could not be gotten.
     */
    public String getName() {
        try {
            return con.getCatalog();
        } catch (SQLException throwables) {
            log.log(Level.FINER, "Error getting database name on database " + cachedName + ".", throwables);
            return cachedName;
        }
    }

    /**
     * Returns the connection to the dataserver.
     * @return The connection to the dataserver.
     */
    public Connection getConnection() {
        return con;
    }

    /**
     * Creates a new statement.
     * <p style="font-size: 40px; color: red; font-weight: bold;">DO NOT FORGET TO CLOSE THIS.</p>
     * @return A new statement which must be closed once finished.
     */
    public Statement createStatement() {
        try {
            return con.createStatement();
        } catch (SQLException throwables) {
            log.log(Level.FINER, "Error creating statement on database " + getName() + ".", throwables);
            return null;
        }
    }

    /**
     * Counts columns in a table.
     * @param table The table to count them in.
     * @param what What columns to count.
     * @param condition The condition the row must meet to be counted.
     * @return The amount of results found or {@code -1} if an error occurred.
     */
    public int count(String table, String what, QueryCondition condition) {
        ResultSet set = executeQuery("SELECT count(" + what + ") FROM " + table + (condition == null ? "" : " " + condition) + ";");
        try {
            set.next();
            int i = set.getInt(1);
            set.getStatement().close();
            return i;
        } catch (SQLException throwables) {
            log.log(Level.FINER, "Error while counting.", throwables);
            return -1;
        }
    }

    /**
     * Truncates (clears) a table.
     * @param table The table to truncate.
     * @see #delete(String, QueryCondition)
     */
    public void truncate(String table) {
        execute("TRUNCATE " + table + ";");
    }

    /**
     * Deletes rows matching the given condition or all when no condition given.
     * @param table The table to delete rows from.
     * @param condition The condition rows must meet in order to be deleted.
     * @return The amount of rows affected.
     * @see #truncate(String)
     */
    public int delete(String table, QueryCondition condition) {
        return executeUpdate("DELETE FROM " + table + (condition == null ? "" : " WHERE " + condition) + ";");
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="font-size: 20; color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @return A raw ResultSet that must be closed after use.
     * @see #select(String, CharSequence, QueryCondition, QueryOrder)
     */
    public ResultSet selectRaw(String table, CharSequence column, QueryCondition condition, QueryOrder order) {
        return selectRaw(table, new CharSequence[] {column}, condition, order);
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="font-size: 20; color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @return A raw ResultSet that must be closed after use.
     * @see #select(String, CharSequence[], QueryCondition, QueryOrder)
     */
    public ResultSet selectRaw(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order) {
        StringBuilder query = new StringBuilder("SELECT ");
        for (CharSequence seq : columns)
            query.append(getAsString(seq)).append(", ");
        query.delete(query.length()-2, query.length()).append(" FROM ").append(table).append(condition == null ? "" : " WHERE " + condition).append(order == null ? "" : " ORDER BY " + order);
        return executeQuery(query.toString());
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @return Parsed data in the form of {@link SelectResults}.
     */
    public SelectResults select(String table, CharSequence column, QueryCondition condition, QueryOrder order) {
        return select(table, new CharSequence[] {column}, condition, order);
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @return Parsed data in the form of {@link SelectResults}.
     */
    public SelectResults select(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order) {
        ResultSet set = selectRaw(table, columns, condition, order);
        List<Map<String, Object>> result = new ArrayList<>();
        if (set != null)
            try {
                while (set.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= set.getMetaData().getColumnCount(); i++) row.put(set.getMetaData().getColumnName(i), set.getObject(i));
                    result.add(row);
                }
                set.getStatement().close();
            } catch (SQLException e) {
                log.log(Level.FINER, "Error iterating through results from table '" + table + "'.", e);
            }
        return new SelectResults(this, table, columns, condition, order, result);
    }

    /**
     * Inserts new data into the table.
     * @param table The table to insert to.
     * @param column The column to insert a value into.
     * @param value The value to insert into the column.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], Object[])
     */
    public int insert(String table, String column, Object value) {
        return insert(table, new String[] {column}, new Object[] {value});
    }

    /**
     * Inserts new data into the table.
     * @param table The table to insert to.
     * @param columns The columns to insert values into.
     * @param values The values to insert into the columns.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], List)
     */
    public int insert(String table, String[] columns, Object[] values) {
        return insert(table, columns, Lists.<Object[]>newArrayList(values));
    }

    /**
     * Inserts new data into the table.
     * @param table The table to insert to.
     * @param columns The columns to insert values into.
     * @param values The values to insert into the columns. Each array in this list is a new row to be inserted.
     * @return The amount of rows affected (added).
     */
    public int insert(String table, String[] columns, List<Object[]> values) {
        StringBuilder query = new StringBuilder("INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ");
        for (Object[] valuesArray : values) {
            query.append("(");
            for (Object value : valuesArray)
                query.append(getAsString(value)).append(", ");
            query.delete(query.length()-2, query.length()).append("), ");
        }
        return executeUpdate(query.delete(query.length()-2, query.length()).append(';').toString());
    }

    /**
     * Inserts new data or edits old data when a row with the given value for the given column already exists.
     * It's like replace, but only replaces a column instead of the whole row.
     * @param table The table to insert into.
     * @param column The column to insert a value into.
     * @param value The value to insert.
     * @param duplicateValue The value to insert when the column already has this value.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map)
     */
    public int insertUpdate(String table, String column, String value, String duplicateValue) {
        return insertUpdate(table, new String[] {column}, new String[] {value}, ImmutableMap.<String, Object>builder().put(column, duplicateValue).build());
    }

    /**
     * Inserts new data or edits old data when a row with the given key already exists.
     * It's like replace, but only replaces a couple columns instead of the whole row.
     * @param table The table to insert into.
     * @param columns The columns to insert values into, one of these should be a {@code PRIMARY KEY} column.
     * @param values The values to insert into the columns.
     * @param duplicateValues The columns to update and the values to update them with when a row with the given key already exists.
     * @return The amount of rows affected.
     */
    public int insertUpdate(String table, String[] columns, Object[] values, Map<String, Object> duplicateValues) {
        StringBuilder query = new StringBuilder("INSERT INTO " + table + " (`" + String.join("`, `", columns) + "`) VALUES (");
        for (Object value : values)
            query.append(getAsString(value)).append(", ");
        query.delete(query.length()-2, query.length()).append(") ON DUPLICATE KEY UPDATE ");
        duplicateValues.forEach((key, value) -> query.append('`').append(key).append('`').append('=').append(getAsString(value)).append(", "));
        if (duplicateValues.size() > 0) query.delete(query.length()-2, query.length());
        query.append(";");
        try (Statement stmt = createStatement()) {
            return stmt.executeUpdate(query.toString());
        } catch (SQLException e) {
            log.log(Level.FINER, "Error executing '" + query + "' on database " + getName() + ".", e);
            return 0;
        }
    }

    /**
     * Inserts new data or edits old data when a row with the given value for the given column already exists.
     * It's like replace, but only replaces a column instead of the whole row.
     * @param table The table to insert into.
     * @param column The column to insert a value into. This must be a PRIMARY KEY column.
     * @param value The value to insert.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map)
     */
    public int insertIgnore(String table, String column, String value) {
        return insertIgnore(table, new String[] {column}, new String[] {value}, column);
    }

    /**
     * Inserts new data or inserts the given key into the keyColumn (which already has that value so it basically ignores it) when a row with the given key already exists.
     * @param table The table to insert into.
     * @param columns The columns to insert values into, one of these should be a {@code PRIMARY KEY} column.
     * @param values The values to insert into the columns.
     * @param keyColumn The PRIMARY KEY column that's used to determine whether to ignore the insertion. This column should also be present in columns and a value for it should be present in values.
     * @return The amount of rows affected.
     */
    public int insertIgnore(String table, String[] columns, Object[] values, String keyColumn) {
        return insertUpdate(table, columns, values, ImmutableMap.<String, Object>builder().put(keyColumn, values[Arrays.binarySearch(columns, keyColumn)]).build());
    }

    /**
     * Updates data in a table.
     * @param table The table to update.
     * @param column The column to update
     * @param value The new value of the column.
     * @param condition The condition rows must meet in order to be updated.
     * @return The amount of rows affected.
     */
    public int update(String table, String column, Object value, QueryCondition condition) {
        return update(table, ImmutableMap.<String, Object>builder().put(column, value).build(), condition);
    }

    /**
     * Updates data in a table.
     * @param table The table to update.
     * @param updates The columns and their corresponding values.
     * @param condition The condition rows must meet in order to be updated.
     * @return The amount of rows affected.
     */
    public int update(String table, Map<String, Object> updates, QueryCondition condition) {
        StringBuilder query = new StringBuilder("UPDATE " + table + " SET ");
        updates.forEach((key, value) -> query.append('`').append(key).append('`').append('=').append(getAsString(value)).append(", "));
        if (updates.size() > 0) query.delete(query.length()-2, query.length());
        if (condition != null) query.append(" WHERE ").append(condition);
        query.append(";");
        return executeUpdate(query.toString());
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists.
     * @param table The table to replace rows in.
     * @param column The column to replace.
     * @param value The value to update.
     * @return The amount of rows affected.
     */
    public int replace(String table, String column, Object value) {
        return replace(table, new String[] {column}, new Object[] {value});
    }

    public int replace(String table, String[] columns, Object[] values) {
        return replace(table, columns, Lists.<Object[]>newArrayList(values));
    }

    public int replace(String table, String[] columns, List<Object[]> values) {
        StringBuilder query = new StringBuilder("REPLACE INTO " + table + " (`" + String.join("`, `", columns) + "`) VALUES ");
        for (Object[] valuesArray : values) {
            query.append("(");
            for (Object o : valuesArray)
                query.append(getAsString(o)).append(", ");
            query.delete(query.length()-2, query.length()).append("), ");
        }
        return executeUpdate(query.delete(query.length()-2, query.length()).append(';').toString());
    }

    /**
     * Drops (completely removes from existence) a table from this database.
     * @param table The name of the table to drop.
     */
    public void drop(String table) {
        execute("DROP TABLE `" + table + "`;");
    }

    /**
     * Executes a query and returns a boolean value which can mean anything.
     * No need to close any statements here.
     * @param query The query to execute.
     * @return A boolean value which can mean anything.
     */
    public boolean execute(String query) {
        try (Statement statement = createStatement()) {
            return statement.execute(query);
        } catch (SQLException e) {
            log.log(Level.FINER, "Error executing '" + query + "' on database " + getName() + ".", e);
            return false;
        }
    }

    /**
     * Executes a query and returns an integer value which often denotes the amount of rows affected.
     * @param query The query to execute.
     * @return An integer value often denoting the amount of rows affected.
     */
    public int executeUpdate(String query) {
        try (Statement statement = createStatement()) {
            return statement.executeUpdate(query);
        } catch (SQLException e) {
            log.log(Level.FINER, "Error executing update '" + query + "' on database " + getName() + ".", e);
            return -1;
        }
    }

    /**
     * Executes a query and returns a ResultSet. Most often used with the SELECT query.
     * <p style="font-size: 25px; color: red; font-weight: bold;">DO NOT FORGET TO CLOSE THE STATEMENT.</p>
     * This can be done with {@code set.getStatement().close()}. Not doing so will eventually result in memory leaks.
     * @param query The query to execute.
     * @return The ResultSet containing all the data this query returned.
     */
    public ResultSet executeQuery(String query) {
        try {
            Statement statement = createStatement();
            statement.executeQuery(query);
            statement.closeOnCompletion();
            return statement.getResultSet();
        } catch (SQLException e) {
            log.log(Level.FINER, "Error executing query '" + query + "' on database " + getName() + ".", e);
            return null;
        }
    }

    /**
     * Creates a table from a preset.
     * @param preset The preset to build.
     * @see TablePreset
     */
    public void createTable(TablePreset preset) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + preset.getName() + " (");
        preset.build().forEach((key, value) -> query.append(key).append(' ').append(value).append(", "));
        preset.getIndices().forEach(index -> query.append(index).append(", "));
        query.delete(query.length() - 2, query.length());
        query.append(");");
        executeUpdate(query.toString());
    }

    /**
     * Checks if a table exists.
     * @param name The name of the table.
     * @return Whether a table by the given name exists.
     */
    public boolean tableExists(String name) {
        try {
            ResultSet set = executeQuery("SHOW TABLES LIKE " + enquote(name) + ";");
            boolean b = set.next();
            set.getStatement().close();
            return b;
        } catch (SQLException throwables) {
            log.log(Level.FINER, "Error checking if table " + enquote(name) + " exists on database " + getName() + ".", throwables);
            return false;
        }
    }

    @Override
    public String toString() {
        return "Database[" +
                "name='" + getName() + '\'' +
                ']';
    }

    /**
     * Puts the given String in quotes and escapes any quotes in it to avoid SQL injection.
     * @param s The String to enquote.
     * @return The given String surrounded by quotes.
     */
    public static String enquote(String s) {
        return "'" + escapeQuotes(s) + "'";
    }

    /**
     * Replaces all single quotes in the string with two single quotes to have MySQL read it as a single quote rather than a string end.
     * @param s The String to escape.
     * @return A String in which all single quotes are now two single quotes.
     */
    public static String escapeQuotes(String s) {
        return s.replace("'", "''");
    }

    /**
     * The same as {@link #escapeQuotes(String)} but does it for a whole array of Strings.
     * @param sa The String array.
     * @return The same array given except all Strings have been escaped. Null values in the array are retained.
     */
    public static String[] escapeQuotes(String[] sa) {
        for (int i = 0; i < sa.length; i++)
            sa[i] = sa[i] == null ? null : escapeQuotes(sa[i]);
        return sa;
    }

    /**
     * Gets a {@link CharSequence} as a String, in case of a {@link QueryFunction} this returns its function, in case of an asterisk this returns an asterisk, in all other cases this returns the given {@link CharSequence} but surrounded by graves.
     * @param seq The sequence to get as String.
     * @return A String, either a {@link QueryFunction}'s function or the given {@link CharSequence} surrounded by graves.
     */
    public static String getAsString(CharSequence seq) {
        return seq instanceof QueryFunction ? ((QueryFunction) seq).getFunction() : "*".contentEquals(seq) ? String.valueOf(seq) : "`" + seq + "`";
    }

    /**
     * Converts an Object to a String to be used in queries. The default cases are as follows:
     * <ul>
     *     <li><b>Null</b>: {@code null}</li>
     *     <li><b>Any number</b>: String representation of said number</li>
     *     <li><b>Byte array</b>: a hex String representing the given bytes</li>
     *     <li><b>{@link QueryFunction}</b>: the {@link QueryFunction}'s function</li>
     *     <li><b>Type registered with {@link #registerTypeConverter(Class, Function)}</b>: the result of the registered type converter</li>
     *     <li><b>Anything else</b>: an {@link #enquote(String) enquoted} String representation</li>
     * </ul>
     * @param o The object to convert.
     * @return A String representation of the given object.
     */
    public static String getAsString(Object o) {
        if (o == null) return "null";
        else if (o instanceof Number) return o.toString();
        else if (o instanceof byte[]) return "0x" + Hex.encodeHexString((byte[]) o).toUpperCase(Locale.ROOT); // For blobs and geometry objects
        else if (o instanceof QueryFunction) return ((QueryFunction) o).getFunction();
        else if (classConverters.containsKey(o.getClass())) return classConverters.get(o.getClass()).apply(o);
        else return enquote(String.valueOf(o));
    }

    /**
     * Register a type converter used to determine how to convert an object of the given {@code Class} to a String which can be used in MySQL queries.
     * @param clazz The type of objects this converter can accept, objects extending this class must be registered separately.
     * @param converter The function accepting the given type and outputting its String representation.
     * @param <T> The type of objects to accept.
     */
    public static <T> void registerTypeConverter(Class<T> clazz, Function<T, String> converter) {
        classConverters.put(clazz, o -> converter.apply((T) o));
    }

    /**
     * Reads a String starting with a single quote until the next quote.
     * @param s The String to read.
     * @return The String between the first set of quotes found.
     */
    // https://stackoverflow.com/a/22789788
    public static String readQuotedString(String s) {
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(s));
        tokenizer.resetSyntax();
        tokenizer.whitespaceChars(0, 32);
        tokenizer.wordChars(33, 255);
        tokenizer.quoteChar('\'');
        StringBuilder builder = new StringBuilder();
        try {
            tokenizer.nextToken();
            return tokenizer.sval;
        } catch (IOException e) { // Impossible
            return null;
        }
    }

    /**
     * Converts a List to a different type.
     * @param list The list whose elements must be converted.
     * @param converter A function accepting elements of list and outputting its representation of the requested type.
     * @param <T> The type of the original List.
     * @param <X> The type of the requested List.
     * @return A list of the requested type.
     * @see #convertMap(Map, Function)
     */
    public static <T, X> List<X> convertList(Iterable<T> list, Function<T, X> converter) {
        List<X> converted = new ArrayList<>();
        list.forEach(t -> converted.add(converter.apply(t)));
        return converted;
    }

    /**
     * Converts a Map to a different type.
     * @param map The map whose entries must be converted.
     * @param converter A bifunction accepting entries of map and outputting a {@link Pair} of new key and new value respectively.
     * @param <OK> The original key type
     * @param <OV> The original value type
     * @param <NK> The new key type
     * @param <NV> The new value type
     * @return A map with the requested types.
     * @see #convertList(Iterable, Function)
     */
    public static <OK, OV, NK, NV> Map<NK, NV> convertMap(Map<OK, OV> map, Function<Map.Entry<OK, OV>, Pair<NK, NV>> converter) {
        Map<NK, NV> converted = map instanceof LinkedHashMap ? new LinkedHashMap<>() : new HashMap<>();
        map.entrySet().forEach(entry -> {
            Pair<NK, NV> pair = converter.apply(entry);
            converted.put(pair.getKey(), pair.getValue());
        });
        return converted;
    }

}
