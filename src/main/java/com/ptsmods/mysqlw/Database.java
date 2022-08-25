package com.ptsmods.mysqlw;

import com.ptsmods.mysqlw.procedure.ProcedureParameter;
import com.ptsmods.mysqlw.procedure.stmt.DelimiterStmt;
import com.ptsmods.mysqlw.procedure.stmt.block.EndStmt;
import com.ptsmods.mysqlw.procedure.stmt.misc.BeginStmt;
import com.ptsmods.mysqlw.query.*;
import com.ptsmods.mysqlw.query.builder.InsertBuilder;
import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.procedure.BlockBuilder;
import com.ptsmods.mysqlw.procedure.TriggeringEvent;
import com.ptsmods.mysqlw.table.TableIndex;
import com.ptsmods.mysqlw.table.TablePreset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Database {
    private static final Map<Connection, Database> databases = new HashMap<>();
    private static final Map<Class<?>, Function<Object, String>> classConverters = new HashMap<>();
    private static final Map<Class<?>, Function<String, Object>> reverseClassConverters = new HashMap<>();
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Downloads the latest version of the connector for the given type and attempts to add it to the classpath.<br>
     * It is not recommended you rely on this, but if, for example, you offer your users a choice whether
     * to use MySQL or SQLite and you do not want to make your jar file huge, there is always this option.
     * @param type The type of the connector to download.
     * @param version The version of the connector to download. If null, automatically downloads the latest one. In case of {@link RDBMS#MySQL MySQL},
     *                this version should correspond with the version of the server you're trying to connect to.
     * @param file The file to download to.
     * @param useCache Whether to use a cached file if the given file already exists. If the given file does not appear to be a connector of the given type, a new version will be downloaded nonetheless.
     * @throws IllegalArgumentException If the given type is {@link RDBMS#UNKNOWN}.
     * @throws IOException If anything went wrong while downloading the file.
     */
    public static void loadConnector(RDBMS type, @Nullable String version, File file, boolean useCache) throws IOException {
        checkNotNull(type, "type");
        checkNotNull(file, "file");
        if (type == RDBMS.UNKNOWN) throw new IllegalArgumentException("The type cannot be UNKNOWN.");
        if (version == null) {
            if (useCache && checkAndAdd(file, type)) return;
            String versionCheck = type.getMetadataUrl();
            URL versionCheckUrl = new URL(versionCheck);
            URLConnection connection = versionCheckUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null && !(line = line.trim()).isEmpty())
                if (line.startsWith("<release>") && line.endsWith("</release>")) {
                    version = line.substring("<release>".length(), line.length() - "</release>".length());
                    break;
                }
            reader.close();
        }
        try (ReadableByteChannel rbc = Channels.newChannel(new URL(Objects.requireNonNull(type.getDownloadUrl(version))).openStream()); FileOutputStream fos = new FileOutputStream(file)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        addToClassPath(file, type.getInitialLoadClass());
    }

    private static void addToClassPath(File file, String initialLoadClass) {
        if (System.getProperty("java.version").startsWith("1.8") && ClassLoader.getSystemClassLoader() instanceof URLClassLoader) {
            // In Java 1.8 the system classloader is a URLClassLoader, starting from Java 9 this is an AppClassLoader.
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method;
            try {
                method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException ignored) {
                return;
            } // Impossible
            method.setAccessible(true);
            try {
                method.invoke(classLoader, file.toURI().toURL());
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) { // Shouldn't happen, but who knows?
                e.printStackTrace();
            }
        } else
            try (URLClassLoader loader = new URLClassLoader(new URL[] {file.toURI().toURL()}, Database.class.getClassLoader())) {
                // This is likely not to work in many cases, but it's simply not easy to do on Java 9+.
                loader.loadClass(initialLoadClass);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ignored) {}
        return false;
    }

    private static boolean checkAndAdd(File file, RDBMS type) throws IOException {
        if (classExists(type.getInitialLoadClass())) return true;
        else if (file.exists()) {
            addToClassPath(file, type.getInitialLoadClass());
            if (!classExists(type.getInitialLoadClass())) loadConnector(type, null, file, false);
            return true;
        }
        return false;
    }

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
        checkNotNull(host, "host");
        checkNotNull(name, "name");

        try {
            Class.forName(RDBMS.MySQL.getInitialLoadClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Could not find MySQL connector on classpath, is it loaded?", e);
        }

        Database db = new Database(RDBMS.MySQL, DriverManager.getConnection(RDBMS.MySQL.formatConnectionUrl(host + ':' + port), username, password), name);
        db.execute("CREATE DATABASE IF NOT EXISTS " + name + ";"); // Create database if it does not yet exist.
        db.getConnection().setCatalog(name);
        return db;
    }

    /**
     * Makes a new connection to an SQLite database or creates it if it does not yet exist.
     * @param file The database file to connect to.
     * @return A Database with which you can do anything.
     * @throws SQLException If an error occurred while either connecting or creating the database.
     */
    public static Database connect(File file) throws SQLException {
        try {
            Class.forName(RDBMS.SQLite.getInitialLoadClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Could not find SQLite connector on classpath, is it loaded?", e);
        }
        return new Database(RDBMS.SQLite, DriverManager.getConnection(RDBMS.SQLite.formatConnectionUrl(file.getAbsolutePath())), file.getName().substring(file.getName().lastIndexOf('.')));
    }

    /**
     * Wraps an SQL connection in a Database. Allows you to connect to any type of database.<br>
     * <p style="font-weight: bold; color: red;">THIS FEATURE IS UNSUPPORTED.</p>
     * @param connection The connection to wrap.
     * @return A new, probably unstable, Database.
     * @see #connect(String, int, String, String, String)
     * @see #connect(File)
     */
    public static Database connect(Connection connection) {
        return new Database(RDBMS.UNKNOWN, connection, "UNKNOWN");
    }

    /**
     * Gets the Database that wraps this connection.
     * @param connection The connection
     * @return The Database that wraps this connection or null.
     */
    public static @Nullable Database getDatabase(Connection connection) {
        return databases.get(connection);
    }

    public static Database getDatabase(ResultSet set) {
        try {
            return getDatabase(set.getStatement().getConnection());
        } catch (SQLException throwables) {
            throw new SilentSQLException(throwables);
        }
    }

    private final RDBMS type;
    private final Connection con;
    private final Logger log;
    private boolean doLog = false;
    private final String cachedName;
    private Executor executor;
    private Function<Throwable, Void> errorHandler;

    private Database(RDBMS type, Connection con, String name) {
        this.type = type;
        this.con = con;
        log = Logger.getLogger("Database-" + name);
        cachedName = name;
        executor = type.getDefaultExecutor(name);
        errorHandler = t -> {
            log.log(Level.SEVERE, "An error occurred during an asynchronous Database call.", t);
            return null;
        };
        databases.put(con, this);
    }

    public Logger getLog() {
        return log;
    }

    public boolean doLog() {
        return doLog;
    }

    /**
     * Sets whether exceptions should be logged or thrown.<br>
     * When this is set to {@code false}, all {@link SQLException}s will be thrown wrapped in a {@link SilentSQLException}.
     * @param doLog Whether to log or throw exceptions.
     */
    public void setLogging(boolean doLog) {
        this.doLog = doLog;
    }

    public void logOrThrow(String msg, SQLException e) throws SilentSQLException {
        if (doLog) log.log(Level.FINER, msg, e);
        else throw new SilentSQLException(e);
    }

    /**
     * @return The {@link Executor} used to run tasks asynchronously.
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Sets the {@link Executor} used to run tasks asynchronously.
     * @param executor The new default executor to use
     * @see Executors
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * @return The default error handler used whenever an asynchronous call throws an error.
     */
    public Consumer<Throwable> getErrorHandler() {
        return errorHandler::apply;
    }

    /**
     * Sets the error handler used whenever an asynchronous call throws an error.
     * @param errorHandler The error handler to use
     */
    public void setErrorHandler(Consumer<Throwable> errorHandler) {
        this.errorHandler = t -> {
            errorHandler.accept(t);
            return null;
        };
    }

    public RDBMS getType() {
        return type;
    }

    /**
     * Attempts to get the name of the database currently in use.
     * @return The name of the currently in use database, or the cached name if it could not be gotten.
     */
    public String getName() throws SilentSQLException {
        try {
            return con.getCatalog();
        } catch (SQLException throwables) {
            logOrThrow("Error getting database name on database " + cachedName + ".", throwables);
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
     * <p style="color: red; font-weight: bold;">DO NOT FORGET TO CLOSE THIS.</p>
     * @return A new statement which must be closed once finished.
     */
    public Statement createStatement() throws SilentSQLException {
        try {
            return con.createStatement();
        } catch (SQLException throwables) {
            logOrThrow("Error creating statement on database " + getName() + ".", throwables);
            return null;
        }
    }

    /**
     * Prepares a new statement.
     * @param query The query to use in this statement. Use question marks as argument placeholders.
     * @return A prepared statement which can be used to easily insert or update data.
     */
    public PreparedStatement prepareStatement(String query) {
        try {
            return con.prepareStatement(query);
        } catch (SQLException throwables) {
            logOrThrow("Could not prepare statement with query '" + query + "'", throwables);
            return null;
        }
    }

    /**
     * Runs the given supplier on the set executor using {@link CompletableFuture}s.
     * @param sup The supplier to run.
     * @param <T> The type the given supplier returns.
     * @return A {@link} CompletableFuture.
     */
    public <T> CompletableFuture<T> runAsync(Supplier<T> sup) {
        Exception rootTrace = new Exception("Trace to root of async call");
        return CompletableFuture.supplyAsync(sup, getExecutor()).exceptionally(t -> {
            errorHandler.apply(new AsyncSQLException(t, rootTrace));
            return null;
        });
    }

    /**
     * Runs the given runnable on the set executor using {@link CompletableFuture}s.
     * @param run The runnable to run.
     * @return A {@link} CompletableFuture.
     */
    private CompletableFuture<Void> runAsync(Runnable run) {
        Exception rootTrace = new Exception("Trace to root of async call");
        return CompletableFuture.runAsync(run, getExecutor()).exceptionally(t -> {
            errorHandler.apply(new AsyncSQLException(t, rootTrace));
            return null;
        });
    }

    /**
     * Counts columns in a table.
     * @param table The table to count them in.
     * @param what What columns to count.
     * @param condition The condition the row must meet to be counted.
     * @return The amount of results found or {@code -1} if an error occurred.
     * @see #countAsync(String, String, QueryCondition)
     */
    public int count(String table, String what, QueryCondition condition) throws SilentSQLException {
        ResultSet set = executeQuery("SELECT count(" + what + ") FROM " + engrave(table) + (condition == null ? "" : " WHERE " + condition) + ";");
        try {
            set.next();
            int i = set.getInt(1);
            set.getStatement().close();
            return i;
        } catch (SQLException throwables) {
            logOrThrow("Error while counting.", throwables);
            return -1;
        }
    }

    /**
     * Counts columns in a table asynchronously.
     * @param table The table to count them in.
     * @param what What columns to count.
     * @param condition The condition the row must meet to be counted.
     * @return The amount of results found or {@code -1} if an error occurred.
     * @see #count(String, String, QueryCondition)
     */
    public CompletableFuture<Integer> countAsync(String table, String what, QueryCondition condition) {
        return runAsync(() -> count(table, what, condition));
    }

    /**
     * Truncates (clears) a table.
     * @param table The table to truncate.
     * @see #delete(String, QueryCondition)
     * @see #truncateAsync(String)
     */
    public void truncate(String table) {
        if (getType() == RDBMS.SQLite) delete(table, null); // No truncate statement in SQLite.
        else execute("TRUNCATE " + engrave(table) + ";");
    }

    /**
     * Truncates (clears) a table asynchronously.
     * @param table The table to truncate.
     * @see #delete(String, QueryCondition)
     * @see #truncate(String)
     */
    public CompletableFuture<Void> truncateAsync(String table) {
        return runAsync(() -> truncate(table));
    }

    /**
     * Deletes rows matching the given condition or all when no condition given.
     * @param table The table to delete rows from
     * @param condition The condition rows must meet in order to be deleted
     * @return The amount of rows affected
     * @see #truncate(String)
     * @see #deleteAsync(String, QueryCondition)
     */
    public int delete(String table, QueryCondition condition) {
        return delete(table, condition, -1);
    }

    /**
     * Deletes rows matching the given condition or all when no condition given.
     * @param table The table to delete rows from
     * @param condition The condition rows must meet in order to be deleted
     * @param limit The maximum amount of rows to delete
     * @return The amount of rows affected.
     * @see #truncate(String)
     * @see #deleteAsync(String, QueryCondition)
     * @throws IllegalStateException When limit > 0 and type is {@link RDBMS#SQLite SQLite}
     * as SQLite does not support delete limits.
     */
    public int delete(String table, QueryCondition condition, int limit) {
        if (limit > 0 && getType() == RDBMS.SQLite)
            throw new IllegalStateException("SQLite does not have native support for delete limits.");

        return executeUpdate("DELETE FROM " + engrave(table) +
                (condition == null ? "" : " WHERE " + condition) + (limit > 0 ? " LIMIT " + limit : "") + ";");
    }

    /**
     * Deletes rows matching the given condition or all when no condition given asynchronously.
     * @param table The table to delete rows from
     * @param condition The condition rows must meet in order to be deleted
     * @return The amount of rows affected.
     * @see #truncate(String)
     * @see #delete(String, QueryCondition)
     */
    public CompletableFuture<Integer> deleteAsync(String table, QueryCondition condition) {
        return deleteAsync(table, condition, -1);
    }

    /**
     * Deletes rows matching the given condition or all when no condition given asynchronously.
     * @param table The table to delete rows from
     * @param condition The condition rows must meet in order to be deleted
     * @param limit The maximum amount of rows to delete
     * @return The amount of rows affected.
     * @see #truncate(String)
     * @see #delete(String, QueryCondition)
     */
    public CompletableFuture<Integer> deleteAsync(String table, QueryCondition condition, int limit) {
        return runAsync(() -> delete(table, condition, limit));
    }


    /**
     * Creates a new {@link SelectBuilder} to use for selecting data.
     * @param table The table to select from.
     * @return A new {@link SelectBuilder}.
     */
    public SelectBuilder selectBuilder(String table) {
        return SelectBuilder.create(this, table);
    }

    /**
     * Creates a new {@link SelectBuilder} to use for selecting data.
     * @param select The select query to select from.
     * @return A new {@link SelectBuilder}.
     */
    public SelectBuilder selectBuilder(SelectBuilder select) {
        return SelectBuilder.create(this, select);
    }

    /**
     * Selects a single variable stored in the RDBMS.
     * @param variable The name of the variable
     * @return The value of the variable
     * @see #selectVariableAsync(String)
     */
    public Object selectVariable(String variable) {
        return SelectResults.parse(executeQuery("SELECT " + variable + ';')).get(0).get(variable);
    }

    /**
     * Asynchronously selects a single variable stored in the RDBMS.
     * @param variable The name of the variable
     * @return The value of the variable
     * @see #selectVariable(String)
     */
    public CompletableFuture<Object> selectVariableAsync(String variable) {
        return runAsync(() -> selectVariable(variable));
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRawAsync(String, CharSequence)
     */
    public ResultSet selectRaw(String table, CharSequence column) {
        return selectBuilder(table).select(column).executeRaw();
    }

    /**
     * Runs a select query and returns the raw output asynchronously.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRawAsync(String, CharSequence)
     */
    public CompletableFuture<ResultSet> selectRawAsync(String table, CharSequence column) {
        return runAsync(() -> selectRaw(table, column));
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRawAsync(String, CharSequence, QueryCondition)
     */
    public ResultSet selectRaw(String table, CharSequence column, QueryCondition condition) {
        return selectBuilder(table).select(column).where(condition).executeRaw();
    }

    /**
     * Runs a select query and returns the raw output asynchronously.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRaw(String, CharSequence, QueryCondition)
     */
    public CompletableFuture<ResultSet> selectRawAsync(String table, CharSequence column, QueryCondition condition) {
        return runAsync(() -> selectRaw(table, column, condition));
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return A raw ResultSet that must be closed after use.
     * @see #select(String, CharSequence, QueryCondition, QueryOrder, QueryLimit)
     * @see #selectRawAsync(String, CharSequence, QueryCondition, QueryOrder, QueryLimit)
     */
    public ResultSet selectRaw(String table, CharSequence column, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return selectBuilder(table).select(column).where(condition).order(order).limit(limit).executeRaw();
    }

    /**
     * Runs a select query and returns the raw output asynchronously.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return A raw ResultSet that must be closed after use.
     * @see #select(String, CharSequence, QueryCondition, QueryOrder, QueryLimit)
     * @see #selectRaw(String, CharSequence, QueryCondition, QueryOrder, QueryLimit)
     */
    public CompletableFuture<ResultSet> selectRawAsync(String table, CharSequence column, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return runAsync(() -> selectRaw(table, column, condition, order, limit));
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRawAsync(String, CharSequence[])
     */
    public ResultSet selectRaw(String table, CharSequence[] columns) {
        return selectBuilder(table).select(columns).executeRaw();
    }

    /**
     * Runs a select query and returns the raw output asynchronously.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRaw(String, CharSequence[])
     */
    public CompletableFuture<ResultSet> selectRawAsync(String table, CharSequence[] columns) {
        return runAsync(() -> selectRaw(table, columns));
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRawAsync(String, CharSequence[], QueryCondition)
     */
    public ResultSet selectRaw(String table, CharSequence[] columns, QueryCondition condition) {
        return selectBuilder(table).select(columns).where(condition).executeRaw();
    }

    /**
     * Runs a select query and returns the raw output asynchronously.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @return A raw ResultSet that must be closed after use.
     * @see #selectRaw(String, CharSequence[], QueryCondition)
     */
    public CompletableFuture<ResultSet> selectRawAsync(String table, CharSequence[] columns, QueryCondition condition) {
        return runAsync(() -> selectRaw(table, columns, condition));
    }

    /**
     * Runs a select query and returns the raw output.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return A raw ResultSet that must be closed after use.
     * @see #select(String, CharSequence[], QueryCondition, QueryOrder, QueryLimit)
     * @see #selectRawAsync(String, CharSequence[], QueryCondition, QueryOrder, QueryLimit)
     */
    public ResultSet selectRaw(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return selectBuilder(table).select(columns).where(condition).order(order).limit(limit).executeRaw();
    }

    /**
     * Runs a select query and returns the raw output asynchronously.
     * <b>The statement used for this query is </b><p style="color: red; font-weight: bold;">not closed</p><b> so make sure to close it with {@code set.getStatement().close()}.</b>
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return A raw ResultSet that must be closed after use.
     * @see #select(String, CharSequence[], QueryCondition, QueryOrder, QueryLimit)
     * @see #selectRaw(String, CharSequence[], QueryCondition, QueryOrder, QueryLimit)
     */
    public CompletableFuture<ResultSet> selectRawAsync(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return runAsync(() -> selectRaw(table, columns, condition, order, limit));
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param column The column to select.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #selectAsync(String, CharSequence)
     */
    public SelectResults select(String table, CharSequence column) {
        return selectBuilder(table).select(column).execute();
    }

    /**
     * Runs a select query and returns parsed output asynchronously.
     * @param table The table to select from.
     * @param column The column to select.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #select(String, CharSequence)
     */
    public CompletableFuture<SelectResults> selectAsync(String table, CharSequence column) {
        return runAsync(() -> select(table, column));
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #selectAsync(String, CharSequence, QueryCondition)
     */
    public SelectResults select(String table, CharSequence column, QueryCondition condition) {
        return selectBuilder(table).select(column).where(condition).execute();
    }

    /**
     * Runs a select query and returns parsed output asynchronously.
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #select(String, CharSequence, QueryCondition)
     */
    public CompletableFuture<SelectResults> selectAsync(String table, CharSequence column, QueryCondition condition) {
        return runAsync(() -> select(table, column, condition));
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #selectAsync(String, CharSequence, QueryCondition, QueryOrder, QueryLimit)
     */
    public SelectResults select(String table, CharSequence column, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return selectBuilder(table).select(column).where(condition).order(order).limit(limit).execute();
    }

    /**
     * Runs a select query and returns parsed output asynchronously.
     * @param table The table to select from.
     * @param column The column to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #select(String, CharSequence, QueryCondition, QueryOrder, QueryLimit)
     */
    public CompletableFuture<SelectResults> selectAsync(String table, CharSequence column, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return runAsync(() -> select(table, column, condition, order, limit));
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #selectAsync(String, CharSequence[])
     */
    public SelectResults select(String table, CharSequence[] columns) {
        return selectBuilder(table).select(columns).execute();
    }

    /**
     * Runs a select query and returns parsed output asynchronously.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #select(String, CharSequence[])
     */
    public CompletableFuture<SelectResults> selectAsync(String table, CharSequence[] columns) {
        return runAsync(() -> select(table, columns));
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #selectAsync(String, CharSequence[], QueryCondition)
     */
    public SelectResults select(String table, CharSequence[] columns, QueryCondition condition) {
        return selectBuilder(table).select(columns).where(condition).execute();
    }

    /**
     * Runs a select query and returns parsed output asynchronously.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #select(String, CharSequence[], QueryCondition)
     */
    public CompletableFuture<SelectResults> selectAsync(String table, CharSequence[] columns, QueryCondition condition) {
        return runAsync(() -> select(table, columns, condition));
    }

    /**
     * Runs a select query and returns parsed output.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #selectAsync(String, CharSequence[], QueryCondition, QueryOrder, QueryLimit)
     */
    public SelectResults select(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return selectBuilder(table).select(columns).where(condition).order(order).limit(limit).execute();
    }

    /**
     * Runs a select query and returns parsed output asynchronously.
     * @param table The table to select from.
     * @param columns The columns to select.
     * @param condition The condition rows must meet in order to be selected.
     * @param order What column to order by and in what direction.
     * @param limit The limit of rows returned, including the offset at which these rows are selected from the entire result.
     * @return Parsed data in the form of {@link SelectResults}.
     * @see #select(String, CharSequence[], QueryCondition, QueryOrder, QueryLimit)
     */
    public CompletableFuture<SelectResults> selectAsync(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order, QueryLimit limit) {
        return runAsync(() -> select(table, columns, condition, order, limit));
    }

    /**
     * Creates a new {@link InsertBuilder} to build insert queries with.
     * @param table The table to insert into.
     * @param columns The columns to insert values into.
     * @return A new {@link InsertBuilder}.
     */
    public InsertBuilder insertBuilder(String table, String... columns) {
        return InsertBuilder.create(this, table, columns);
    }
    
    /**
     * Inserts new data into the table.
     * @param table The table to insert into.
     * @param column The column to insert a value into.
     * @param value The value to insert into the column.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], Object[])
     * @see #insertAsync(String, String, Object)
     */
    public int insert(String table, String column, Object value) {
        return insertBuilder(table, column).insert(value).execute();
    }

    /**
     * Inserts new data into the table asynchronously.
     * @param table The table to insert into.
     * @param column The column to insert a value into.
     * @param value The value to insert into the column.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], Object[])
     * @see #insert(String, String, Object)
     */
    public CompletableFuture<Integer> insertAsync(String table, String column, Object value) {
        return runAsync(() -> insert(table, column, value));
    }

    /**
     * Inserts new data into the table.
     * @param table The table to insert into.
     * @param columns The columns to insert values into.
     * @param values The values to insert into the columns.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], List)
     * @see #insertAsync(String, String[], Object[])
     */
    public int insert(String table, String[] columns, Object[] values) {
        return insertBuilder(table, columns).insert(values).execute();
    }

    /**
     * Inserts new data into the table asynchronously.
     * @param table The table to insert into.
     * @param columns The columns to insert values into.
     * @param values The values to insert into the columns.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], List)
     * @see #insert(String, String[], Object[])
     */
    public CompletableFuture<Integer> insertAsync(String table, String[] columns, Object[] values) {
        return runAsync(() -> insert(table, columns, values));
    }

    /**
     * Inserts new data into the table.
     * @param table The table to insert into.
     * @param columns The columns to insert values into.
     * @param values The values to insert into the columns. Each array in this list is a new row to be inserted.
     * @return The amount of rows affected (added).
     * @see #insertAsync(String, String[], List)
     */
    public int insert(String table, String[] columns, List<Object[]> values) {
        return insertBuilder(table, columns).insert(values).execute();
    }

    /**
     * Inserts new data into the table asynchronously.
     * @param table The table to insert into.
     * @param columns The columns to insert values into.
     * @param values The values to insert into the columns. Each array in this list is a new row to be inserted.
     * @return The amount of rows affected (added).
     * @see #insert(String, String[], List)
     */
    public CompletableFuture<Integer> insertAsync(String table, String[] columns, List<Object[]> values) {
        return runAsync(() -> insert(table, columns, values));
    }

    /**
     * Inserts new data or edits old data when a row with the given value for the given column already exists.
     * It's like replace, but only replaces a column instead of the whole row.
     * @param table The table to insert into.
     * @param column The column to insert a value into.
     * @param value The value to insert.
     * @param duplicateValue The value to insert when the column already has this value.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map, String)
     * @see #insertUpdateAsync(String, String, Object, Object)
     */
    public int insertUpdate(String table, String column, Object value, Object duplicateValue) {
        return insertUpdate(table, new String[] {column}, new Object[] {value}, singletonMap(column, duplicateValue), column);
    }

    /**
     * Inserts new data or edits old data when a row with the given value for the given column already exists asynchronously.
     * It's like replace, but only replaces a column instead of the whole row.
     * @param table The table to insert into.
     * @param column The column to insert a value into.
     * @param value The value to insert.
     * @param duplicateValue The value to insert when the column already has this value.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map, String)
     * @see #insertUpdateAsync(String, String, Object, Object)
     */
    public CompletableFuture<Integer> insertUpdateAsync(String table, String column, Object value, Object duplicateValue) {
        return runAsync(() -> insertUpdate(table, column, value, duplicateValue));
    }

    /**
     * Inserts new data or edits old data when a row with the given key already exists.
     * It's like replace, but only replaces a couple columns instead of the whole row.
     * @param table The table to insert into.
     * @param columns The columns to insert values into, one of these should be a {@code PRIMARY KEY} column.
     * @param values The values to insert into the columns.
     * @param duplicateValues The columns to update and the values to update them with when a row with the given key already exists.
     * @param keyColumn The name of the PRIMARY KEY column. Only has to be set when the type of this Database is {@link RDBMS#SQLite SQLite}, can be {@code null} otherwise.
     * @return The amount of rows affected.
     * @see #insertUpdateAsync(String, String[], Object[], Map, String)
     */
    public int insertUpdate(String table, String[] columns, Object[] values, Map<String, Object> duplicateValues, String keyColumn) throws SilentSQLException {
        StringBuilder query = new StringBuilder("INSERT INTO " + engrave(table) + " (`" + String.join("`, `", columns) + "`) VALUES (");
        for (Object value : values)
            query.append(getAsString(value)).append(", ");
        query.delete(query.length()-2, query.length()).append(") ON ").append(type == RDBMS.SQLite ? "CONFLICT(`" + keyColumn + "`) DO UPDATE SET " : "DUPLICATE KEY UPDATE ");
        duplicateValues.forEach((key, value) -> query.append('`').append(key).append('`').append('=').append(getAsString(value)).append(", "));
        if (duplicateValues.size() > 0) query.delete(query.length()-2, query.length());
        query.append(";");

        return executeUpdate(query.toString());
    }

    /**
     * Inserts new data or edits old data when a row with the given key already exists asynchronously.
     * It's like replace, but only replaces a couple columns instead of the whole row.
     * @param table The table to insert into.
     * @param columns The columns to insert values into, one of these should be a {@code PRIMARY KEY} column.
     * @param values The values to insert into the columns.
     * @param duplicateValues The columns to update and the values to update them with when a row with the given key already exists.
     * @param keyColumn The name of the PRIMARY KEY column. Only has to be set when the type of this Database is {@link RDBMS#SQLite SQLite}, can be {@code null} otherwise.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map, String)
     */
    public CompletableFuture<Integer> insertUpdateAsync(String table, String[] columns, Object[] values, Map<String, Object> duplicateValues, String keyColumn) throws SilentSQLException {
        return runAsync(() -> insertUpdate(table, columns, values, duplicateValues, keyColumn));
    }

    /**
     * Inserts new data or edits old data when a row with the given value for the given column already exists.
     * It's like replace, but only replaces a column instead of the whole row.
     * @param table The table to insert into.
     * @param column The column to insert a value into. This must be a PRIMARY KEY column.
     * @param value The value to insert.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map, String)
     * @see #insertIgnoreAsync(String, String, Object)
     */
    public int insertIgnore(String table, String column, Object value) {
        return insertIgnore(table, new String[] {column}, new Object[] {value}, column);
    }

    /**
     * Inserts new data or edits old data when a row with the given value for the given column already exists asynchronously.
     * It's like replace, but only replaces a column instead of the whole row.
     * @param table The table to insert into.
     * @param column The column to insert a value into. This must be a PRIMARY KEY column.
     * @param value The value to insert.
     * @return The amount of rows affected.
     * @see #insertUpdate(String, String[], Object[], Map, String)
     * @see #insertIgnore(String, String, Object)
     */
    public CompletableFuture<Integer> insertIgnoreAsync(String table, String column, Object value) {
        return runAsync(() -> insertIgnore(table, column, value));
    }

    /**
     * Inserts new data or inserts the given key into the keyColumn (which already has that value so it basically ignores it) when a row with the given key already exists.
     * @param table The table to insert into.
     * @param columns The columns to insert values into, one of these should be a {@code PRIMARY KEY} column.
     * @param values The values to insert into the columns.
     * @param keyColumn The PRIMARY KEY column that's used to determine whether to ignore the insertion. This column should also be present in columns and a value for it should be present in values.
     * @return The amount of rows affected.
     * @see #insertIgnoreAsync(String, String[], Object[], String)
     */
    public int insertIgnore(String table, String[] columns, Object[] values, String keyColumn) {
        return insertUpdate(table, columns, values, singletonMap(keyColumn, values[Arrays.binarySearch(columns, keyColumn)]), keyColumn);
    }

    /**
     * Inserts new data or inserts the given key into the keyColumn (which already has that value so it basically ignores it) when a row with the given key already exists asynchronously.
     * @param table The table to insert into.
     * @param columns The columns to insert values into, one of these should be a {@code PRIMARY KEY} column.
     * @param values The values to insert into the columns.
     * @param keyColumn The PRIMARY KEY column that's used to determine whether to ignore the insertion. This column should also be present in columns and a value for it should be present in values.
     * @return The amount of rows affected.
     * @see #insertIgnore(String, String[], Object[], String)
     */
    public CompletableFuture<Integer> insertIgnoreAsync(String table, String[] columns, Object[] values, String keyColumn) {
        return runAsync(() -> insertIgnore(table, columns, values, keyColumn));
    }

    /**
     * Updates data in a table.
     * @param table The table to update.
     * @param column The column to update
     * @param value The new value of the column.
     * @param condition The condition rows must meet in order to be updated.
     * @return The amount of rows affected.
     * @see #updateAsync(String, String, Object, QueryCondition)
     */
    public int update(String table, String column, Object value, QueryCondition condition) {
        return update(table, singletonMap(column, value), condition);
    }

    /**
     * Updates data in a table asynchronously.
     * @param table The table to update.
     * @param column The column to update
     * @param value The new value of the column.
     * @param condition The condition rows must meet in order to be updated.
     * @return The amount of rows affected.
     * @see #update(String, String, Object, QueryCondition)
     */
    public CompletableFuture<Integer> updateAsync(String table, String column, Object value, QueryCondition condition) {
        return runAsync(() -> update(table, column, value, condition));
    }

    /**
     * Updates data in a table.
     * @param table The table to update.
     * @param updates The columns and their corresponding values.
     * @param condition The condition rows must meet in order to be updated.
     * @return The amount of rows affected.
     * @see #updateAsync(String, Map, QueryCondition)
     */
    public int update(String table, Map<String, Object> updates, QueryCondition condition) {
        StringBuilder query = new StringBuilder("UPDATE " + engrave(table) + " SET ");
        updates.forEach((key, value) -> query.append('`').append(key).append('`').append('=').append(getAsString(value)).append(", "));
        if (updates.size() > 0) query.delete(query.length()-2, query.length());
        if (condition != null) query.append(" WHERE ").append(condition);
        query.append(";");
        return executeUpdate(query.toString());
    }

    /**
     * Updates data in a table asynchronously.
     * @param table The table to update.
     * @param updates The columns and their corresponding values.
     * @param condition The condition rows must meet in order to be updated.
     * @return The amount of rows affected.
     * @see #update(String, Map, QueryCondition)
     */
    public CompletableFuture<Integer> updateAsync(String table, Map<String, Object> updates, QueryCondition condition) {
        return runAsync(() -> update(table, updates, condition));
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists.
     * @param table The table to replace rows in.
     * @param column The column to replace.
     * @param value The value to update.
     * @return The amount of rows affected.
     * @see #replaceAsync(String, String, Object)
     */
    public int replace(String table, String column, Object value) {
        return replace(table, new String[] {column}, new Object[] {value});
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists asynchronously.
     * @param table The table to replace rows in.
     * @param column The column to replace.
     * @param value The value to update.
     * @return The amount of rows affected.
     * @see #replace(String, String, Object)
     */
    public CompletableFuture<Integer> replaceAsync(String table, String column, Object value) {
        return runAsync(() -> replace(table, column, value));
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists.
     * @param table The table to replace rows in.
     * @param columns The columns to replace.
     * @param values The values to update.
     * @return The amount of rows affected.
     * @see #replaceAsync(String, String[], Object[])
     */
    public int replace(String table, String[] columns, Object[] values) {
        return replace(table, columns, Collections.singletonList(values));
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists asynchronously.
     * @param table The table to replace rows in.
     * @param columns The columns to replace.
     * @param values The values to update.
     * @return The amount of rows affected.
     * @see #replace(String, String[], Object[])
     */
    public CompletableFuture<Integer> replaceAsync(String table, String[] columns, Object[] values) {
        return runAsync(() -> replace(table, columns, values));
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists.
     * @param table The table to replace rows in.
     * @param columns The columns to replace.
     * @param values The values to update.
     * @return The amount of rows affected.
     * @see #replaceAsync(String, String[], List)
     */
    public int replace(String table, String[] columns, List<Object[]> values) {
        return insertBuilder(table, columns).insert(values).executeReplace();
    }

    /**
     * Replaces data in a table when a row with the same value for the primary key column as the value given already exists asynchronously.
     * @param table The table to replace rows in.
     * @param columns The columns to replace.
     * @param values The values to update.
     * @return The amount of rows affected.
     * @see #replace(String, String[], List)
     */
    public CompletableFuture<Integer> replaceAsync(String table, String[] columns, List<Object[]> values) {
        return runAsync(() -> replace(table, columns, values));
    }

    /**
     * Drops (completely removes from existence) a table from this database.
     * @param table The name of the table to drop.
     * @see #dropAsync(String)
     */
    public void drop(String table) {
        execute("DROP TABLE " + engrave(table) + ";");
    }

    /**
     * Drops (completely removes from existence) a table from this database asynchronously.
     * @param table The name of the table to drop.
     * @see #drop(String)
     */
    public CompletableFuture<Void> dropAsync(String table) {
        return runAsync(() -> drop(table));
    }

    /**
     * Executes a query and returns a boolean value which can mean anything.
     * No need to close any statements here.
     * @param query The query to execute.
     * @return A boolean value which can mean anything.
     * @see #executeAsync(String)
     */
    public boolean execute(String query) throws SilentSQLException {
        try (Statement statement = createStatement()) {
            return statement.execute(query);
        } catch (SQLException e) {
            logOrThrow("Error executing '" + query + "' on database " + getName() + ".", e);
            return false;
        }
    }

    /**
     * Executes a query and returns a boolean value which can mean anything asynchronously.
     * No need to close any statements here.
     * @param query The query to execute.
     * @return A boolean value which can mean anything.
     * @see #execute(String)
     */
    public CompletableFuture<Boolean> executeAsync(String query) {
        return runAsync(() -> execute(query));
    }

    /**
     * Executes a query and returns an integer value which often denotes the amount of rows affected.
     * @param query The query to execute.
     * @return An integer value often denoting the amount of rows affected.
     * @see #executeUpdateAsync(String)
     */
    public int executeUpdate(String query) throws SilentSQLException {
        try (Statement statement = createStatement()) {
            return statement.executeUpdate(query);
        } catch (SQLException e) {
            logOrThrow("Error executing update '" + query + "' on database " + getName() + ".", e);
            return -1;
        }
    }

    /**
     * Executes a query and returns an integer value which often denotes the amount of rows affected asynchronously.
     * @param query The query to execute.
     * @return An integer value often denoting the amount of rows affected.
     * @see #executeUpdate(String)
     */
    public CompletableFuture<Integer> executeUpdateAsync(String query) {
        return runAsync(() -> executeUpdate(query));
    }

    /**
     * Executes a query and returns a ResultSet. Most often used with the SELECT query.
     * <p style="color: red; font-weight: bold;">DO NOT FORGET TO CLOSE THE STATEMENT.</p>
     * This can be done with {@code set.getStatement().close()}. Not doing so will eventually result in memory leaks.
     * @param query The query to execute.
     * @return The ResultSet containing all the data this query returned.
     * @see #executeQueryAsync(String)
     */
    public ResultSet executeQuery(String query) throws SilentSQLException {
        try {
            Statement statement = createStatement();
            ResultSet set = statement.executeQuery(query);
            statement.closeOnCompletion();
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
            logOrThrow("Error executing query '" + query + "' on database " + getName() + ".", e);
            return null;
        }
    }

    /**
     * Executes a query and returns a ResultSet asynchronously. Most often used with the SELECT query.
     * <p style="color: red; font-weight: bold;">DO NOT FORGET TO CLOSE THE STATEMENT.</p>
     * This can be done with {@code set.getStatement().close()}. Not doing so will eventually result in memory leaks.
     * @param query The query to execute.
     * @return The ResultSet containing all the data this query returned.
     * @see #executeQuery(String)
     */
    public CompletableFuture<ResultSet> executeQueryAsync(String query) {
        return runAsync(() -> executeQuery(query));
    }

    /**
     * Creates a table from a preset.
     * @param preset The preset to build.
     * @see TablePreset
     * @see #createTableAsync(TablePreset)
     */
    public void createTable(TablePreset preset) {
        executeUpdate(preset.buildQuery(type));
    }

    /**
     * Creates a table from a preset asynchronously.
     * @param preset The preset to build.
     * @see TablePreset
     * @see #createTable(TablePreset)
     */
    public CompletableFuture<Void> createTableAsync(TablePreset preset) {
        return runAsync(() -> createTable(preset));
    }

    /**
     * Checks if a table exists.
     * @param name The name of the table.
     * @return Whether a table by the given name exists.
     * @see #tableExistsAsync(String)
     */
    public boolean tableExists(String name) throws SilentSQLException {
        try {
            ResultSet set = con.getMetaData().getTables(null, null, name, null);
            boolean b = set.next();
            set.close();
            return b;
        } catch (SQLException throwables) {
            logOrThrow("Error checking if table " + enquote(name) + " exists on database " + getName() + ".", throwables);
            return false;
        }
    }

    /**
     * Checks if a table exists asynchronously.
     * @param name The name of the table.
     * @return Whether a table by the given name exists.
     * @see #tableExists(String)
     */
    public CompletableFuture<Boolean> tableExistsAsync(String name) {
        return runAsync(() -> tableExists(name));
    }

    /**
     * @param table The table to get the creation query of.
     * @return The query used to create this table.
     * @see #getCreateQueryAsync(String)
     */
    public String getCreateQuery(String table) {
        String query = type == RDBMS.SQLite ? "SELECT sql FROM sqlite_master WHERE name=" + enquote(table) + ";" : "SHOW CREATE TABLE " + engrave(table) + ";";
        SelectResults results = SelectResults.parse(this, table, executeQuery(query), type == RDBMS.SQLite ? QueryCondition.equals("name", table) : null, null, null);
        return results.get(0).get(results.getColumns().get(0)).toString();
    }

    /**
     * @param table The table to get the creation query of.
     * @return The query used to create this table asynchronously.
     * @see #getCreateQuery(String)
     */
    public CompletableFuture<String> getCreateQueryAsync(String table) {
        return runAsync(() -> getCreateQuery(table));
    }

    /**
     * Create a new index on an existing column in an existing table.
     * This is the only way to create indices on SQLite.
     * @param table The table to create the index on.
     * @param index The index to create.
     * @see #createIndexAsync(String, TableIndex)
     */
    public void createIndex(String table, TableIndex index) {
        if (index.getName() == null || index.getName().isEmpty()) throw new IllegalArgumentException("When creating a standalone index, the index must have a name.");
        execute("CREATE " + index.toString(false) + "ON " + engrave(table) + " (" + engrave(index.getColumn()) + ");");
    }

    /**
     * Create a new index on an existing column in an existing table asynchronously.
     * This is the only way to create indices on SQLite.
     * @param table The table to create the index on.
     * @param index The index to create.
     * @see #createIndex(String, TableIndex)
     */
    public CompletableFuture<Void> createIndexAsync(String table, TableIndex index) {
        return runAsync(() -> createIndex(table, index));
    }

    /**
     * Creates a new trigger with the given statements.
     * @param name The name of the trigger
     * @param table The table to create the trigger on
     * @param event The event which will trigger this trigger
     * @param trigger The statements to execute when this trigger is triggered
     * @see #createTriggerAsync(String, String, TriggeringEvent, BlockBuilder)
     */
    public void createTrigger(String name, String table, TriggeringEvent event, BlockBuilder trigger) {
        String query = "DELIMITER $$\n" +
                "CREATE TRIGGER " +
                enquote(name) +
                event.name().replace('_', ' ') +
                " ON " +
                enquote(table) +
                " FOR EACH ROW\n" +
                "BEGIN" +
                trigger +
                "END $$\n" +
                "DELIMITER ;";

        execute(query);
    }

    /**
     * Asynchronously creates a new trigger with the given statements.
     * @param name The name of the trigger
     * @param table The table to create the trigger on
     * @param event The event which will trigger this trigger
     * @param trigger The statements to execute when this trigger is triggered
     * @see #createTrigger(String, String, TriggeringEvent, BlockBuilder)
     * @return A CompletableFuture
     */
    public CompletableFuture<Void> createTriggerAsync(String name, String table, TriggeringEvent event, BlockBuilder trigger) {
        return runAsync(() -> createTrigger(name, table, event, trigger));
    }

    /**
     * Creates a new procedure with the given statements.
     * @param name The name of the procedure
     * @param parameters The parameters of the procedure
     * @param procedure The statements to execute when this procedure is called
     * @see #createProcedureAsync(String, ProcedureParameter[], BlockBuilder)
     */
    public void createProcedure(String name, ProcedureParameter[] parameters, BlockBuilder procedure) {
        StringBuilder builder = new StringBuilder("DELIMITER $$\nCREATE PROCEDURE ").append(name).append('(');

        for (ProcedureParameter parameter : parameters) builder.append(parameter);
        builder.append(')');
        procedure.stmt(0, BeginStmt.begin());
        procedure.stmt(EndStmt.end("$$"));
        procedure.stmt(DelimiterStmt.delimiter(";"));
        execute(builder.toString());
    }

    /**
     * Asynchronously creates a new procedure with the given statements.
     * @param name The name of the procedure
     * @param parameters The parameters of the procedure
     * @param procedure The statements to execute when this procedure is called
     * @see #createProcedure(String, ProcedureParameter[], BlockBuilder)
     * @return A CompletableFuture
     */
    public CompletableFuture<Void> createProcedureAsync(String name, ProcedureParameter[] parameters, BlockBuilder procedure) {
        return runAsync(() -> createProcedure(name, parameters, procedure));
    }

    /**
     * Calls a procedure with the given parameters.
     * @param procedure The name of the procedure to call
     * @param parameters The parameters to pass when calling the procedure
     * @see #callAsync(String, Object...)
     */
    public void call(String procedure, Object... parameters) {
        execute(String.format("CALL %s(%s);", procedure, Arrays.stream(parameters).map(Database::getAsString).collect(Collectors.joining(", "))));
    }

    /**
     * Asynchronously calls a procedure with the given parameters.
     * @param procedure The name of the procedure to call
     * @param parameters The parameters to pass when calling the procedure
     * @see #call(String, Object...)
     * @return A CompletableFuture
     */
    public CompletableFuture<Void> callAsync(String procedure, Object... parameters) {
        return runAsync(() -> call(procedure, parameters));
    }

    @Override
    public String toString() {
        return "Database[" +
                "name='" + getName() + '\'' +
                ']';
    }

    // UTILITY METHODS

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

    public static String engrave(String s) {
        return '`' + s.replace(".", "`.`") + "`";
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
     * Gets a {@link CharSequence} as a String, in case of a {@link QueryFunction} this returns its function,
     * in case of an asterisk this returns an asterisk, in all other cases this returns the given {@link CharSequence} but surrounded by graves.
     * @param seq The sequence to get as String.
     * @return A String, either a {@link QueryFunction}'s function or the given {@link CharSequence} surrounded by graves.
     */
    public static String getAsString(CharSequence seq) {
        return seq instanceof QueryFunction ? ((QueryFunction) seq).getFunction() : "*".contentEquals(seq) ? String.valueOf(seq) : engrave(String.valueOf(seq));
    }

    /**
     * Converts an Object to a String to be used in queries. The default cases are as follows:
     * <ul>
     *     <li><b>Null</b>: {@code null}</li>
     *     <li><b>A boolean</b>: String representation of said boolean in uppercase</li>
     *     <li><b>Any number</b>: String representation of said number</li>
     *     <li><b>Byte array</b>: a hex String representing the given bytes</li>
     *     <li><b>{@link QueryFunction}</b>: the {@link QueryFunction}'s function</li>
     *     <li><b>Type registered with {@link #registerTypeConverter(Class, Function, Function)}</b>: the result of the registered type converter</li>
     *     <li><b>Anything else</b>: an {@link #enquote(String) enquoted} String representation</li>
     * </ul>
     * @param o The object to convert.
     * @return A String representation of the given object.
     */
    public static String getAsString(Object o) {
        if (o == null) return "null";
        else if (o instanceof Boolean) return o.toString().toUpperCase(Locale.ROOT);
        else if (o instanceof Number) return o.toString();
        else if (o instanceof byte[]) return "0x" + encodeHex((byte[]) o).toUpperCase(Locale.ROOT); // For blobs and geometry objects
        else if (o instanceof QueryFunction) return ((QueryFunction) o).getFunction();
        else if (o instanceof UUID) return enquote(o.toString());
        else if (classConverters.containsKey(o.getClass())) return classConverters.get(o.getClass()).apply(o);
        else return enquote(String.valueOf(o));
    }

    private static String encodeHex(byte[] bytes) {
        final char[] out = new char[bytes.length*2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            out[j++] = HEX_DIGITS[(0xF0 & bytes[i]) >>> 4];
            out[j++] = HEX_DIGITS[0x0F & bytes[i]];
        }
        return new String(out);
    }

    /**
     * Converts a String
     * @param s The String to parse.
     * @param clazz The type of the object you wish to parse.
     * @param <T> The generic type of the object.
     * @return An object
     */
    public static <T> T getFromString(String s, Class<T> clazz) {
        return reverseClassConverters.containsKey(clazz) ? clazz.cast(reverseClassConverters.get(clazz).apply(s)) : reverseClassConverters.entrySet().stream()
                .filter(entry -> entry.getKey().isAssignableFrom(clazz))
                .min((e1, e2) -> e1.getKey() == clazz && e2.getKey() == clazz || // Prioritise converters that are of the exact type.
                        e1.getKey() != clazz && e2.getKey() != clazz ? 0 : e1.getKey() == clazz ? 1 : -1)
                .map(entry -> clazz.cast(entry.getValue().apply(s)))
                .orElseThrow(() -> new IllegalArgumentException("Class " + clazz.getName() + " has no registered type converters."));
    }

    /**
     * Register a type converterTo used to determine how to convert an object of the given {@code Class} to a String which can be used in MySQL queries.
     * @param clazz The type of objects this converterTo can accept, objects extending this class must be registered separately.
     * @param converterTo The function accepting the given type and outputting its String representation.
     * @param <T> The type of objects to accept.
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerTypeConverter(Class<T> clazz, Function<T, String> converterTo, Function<String, T> converterFrom) {
        classConverters.put(clazz, o -> converterTo.apply((T) o));
        reverseClassConverters.put(clazz, converterFrom::apply);
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
        try {
            tokenizer.nextToken();
            return tokenizer.sval;
        } catch (IOException e) { // Impossible
            return null;
        }
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(key, value);
        return Collections.unmodifiableMap(map);
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null)
            throw new NullPointerException(String.valueOf(errorMessage));
        return reference;
    }

    public enum RDBMS {
        MySQL("com.mysql.cj.jdbc.Driver", "https://repo1.maven.org/maven2/mysql/mysql-connector-java/maven-metadata.xml",
                "https://repo1.maven.org/maven2/mysql/mysql-connector-java/${VERSION}/mysql-connector-java-${VERSION}.jar",
                "jdbc:mysql://%s/?autoReconnect=true",
                name -> Executors.newCachedThreadPool(r -> new Thread(r, "Database Thread - " + name))),
        SQLite("org.sqlite.JDBC", "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/maven-metadata.xml",
                "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/${VERSION}/sqlite-jdbc-${VERSION}.jar",
                "jdbc:sqlite:%s",
                name -> Executors.newFixedThreadPool(1, r -> new Thread(r, "Database Thread - " + name))), // Preventing database lock, only one thread can use an SQLite database at a time.
        UNKNOWN(null, null, null, null, name -> Executors.newCachedThreadPool(r -> new Thread(r, "Database Thread - " + name)));

        private final String initialLoadClass, metadataUrl, downloadUrl, connectionUrl;
        private final Function<String, Executor> defaultExecutor;

        RDBMS(String initialLoadClass, String metadataUrl, String downloadUrl, String connectionUrl, Function<String, Executor> defaultExecutor) {
            this.initialLoadClass = initialLoadClass;
            this.metadataUrl = metadataUrl;
            this.downloadUrl = downloadUrl;
            this.connectionUrl = connectionUrl;
            this.defaultExecutor = defaultExecutor;
        }

        public String getInitialLoadClass() {
            return initialLoadClass;
        }

        public String getMetadataUrl() {
            return metadataUrl;
        }

        public String getDownloadUrl(String version) {
            return downloadUrl == null ? null : downloadUrl.replace("${VERSION}", version);
        }

        @NotNull
        public String formatConnectionUrl(String host) {
            return connectionUrl == null ? "" : String.format(connectionUrl, host);
        }

        public Executor getDefaultExecutor(String name) {
            return defaultExecutor.apply(name);
        }
    }

}
