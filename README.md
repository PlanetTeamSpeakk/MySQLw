# MySQLw

[![Latest release](https://img.shields.io/github/release/PlanetTeamSpeakk/MySQLw.svg)](https://github.com/PlanetTeamSpeakk/MySQLw/releases/latest)
[![Build Status](https://api.travis-ci.org/PlanetTeamSpeakk/MySQLw.svg)](https://travis-ci.org/PlanetTeamSpeakk/MySQLw)
[![Lines of code](https://img.shields.io/tokei/lines/github/PlanetTeamSpeakk/MySQLw?color=%23fe7d37)](#)
### Link to [Javadoc](https://mysqlw.ptsmods.com)

A wrapper for MySQL and SQLite connections for Java to make your life a lot easier and a lot saner when working with queries.  
This library is merely a wrapper for the default Java SQL library intended to be used with MySQL (or MariaDB for that matter) or SQLite, this means that you also need the MySQL Java connector or the SQLite Java connector to actually connect to your database, but the `Database#loadConnector(RDBMS, File, boolean)` method allows you to add it at runtime.

* [Adding MySQLw to your project](#adding-mysqlw-to-your-project)
  + [Gradle](#gradle)
  + [Maven](#maven)
* [Usage](#usage)
  + [Connecting](#connecting)
  + [Selecting](#selecting)
  + [Inserting](#inserting)
  + [Updating](#updating)
  + [Replacing](#replacing)
  + [Creating tables](#creating-tables)
  + [Database-backed collections](#database-backed-collections)
  + [Type conversion](#type-conversion)
  + [Other smaller features](#other-smaller-features)
* [Async](#async)

## Adding MySQLw to your project
### Gradle
To add MySQLw to your Gradle project, add the following line to your dependencies:
```gradle
implementation 'com.ptsmods:mysqlw:1.7'
```

### Maven
To add MySQLw to your Maven project, add the following code segment to your pom.xml file:
```xml
<dependency>
  <groupId>com.ptsmods</groupId>
  <artifactId>mysqlw</artifactId>
  <version>1.7</version>
</dependency>
```

## Usage
### Connecting
#### MySQL
To start off, you should connect to your database. Assuming the MySQL Java connector is on your classpath too, you can do so with the following code:
```java
String host = "localhost";
int port = 3306;
String name = "mydatabase";
String username = "root";
String password = null;
Database db = Database.connect(host, port, name, username, password);
```

#### SQLite
To connect to an SQLite database, all you have to do is supply the location of the database file.  
You can do so like so:
```java
Database db = Database.connect(new File("sqlite.db"));
```

### Loading the proper connector
Instead of shadowing the connector library for the Relation Database Management System (RDBMS), you can download and load it with `Database#loadConnector(RDBMS, File, boolean)`.  
For instance, if you wish to download the MySQL connector and add it to the classpath, you can use
```java
Database.loadConnector(Database.RDBMS.MySQL, "8.0.23", new File("mysql-connector.jar"), true);
```
This will download the MySQL connector to `mysql-connector.jar` if it has not yet been downloaded to that location (if it has, it will ignore the downloading step as stated by the last parameter, the `useCache` boolean) and then add it to the classpath and verify that it worked.  
If you use this method and it did not successfully manage to load the connector, it will likely throw an `IOException`.  
It is not guaranteed that it will throw an exception on failure as an exception is only thrown while downloading, although without the connector, you'll get all kinds of other problems thrown anyway, so you'll find out nonetheless.

### Selecting
Selecting data is done easily. All data is parsed into a `SelectResults` object which is a list of `SelectResultsRow` which is a map of String keys and Object values.  
Say you have the following table named people (credits to [TutorialRepublic](https://www.tutorialrepublic.com/php-tutorial/php-mysql-select-query.php)):
```
+----+------------+-----------+----------------------+
| id | first_name | last_name | email                |
+----+------------+-----------+----------------------+
|  1 | Peter      | Parker    | peterparker@mail.com |
|  2 | John       | Rambo     | johnrambo@mail.com   |
|  3 | Clark      | Kent      | clarkkent@mail.com   |
|  4 | John       | Carter    | johncarter@mail.com  |
|  5 | Harry      | Potter    | harrypotter@mail.com |
+----+------------+-----------+----------------------+
```
If you wish to select the `last_name` column only you can do so with the following code segment:
```java
SelectResults results = db.select("people", "last_name");
```
Now if you wish to do the same, but only if the id is greater than 3, you could use
```java
SelectResults results = db.select("people", "last_name", QueryCondition.greater("id", 3));
```
Now if you wish to only return results whose email address starts with 'john' and order by first_name and you also want the first_name column returned and only want at most two elements, you can do so with
```java
SelectResults results = db.select("people", new String[] {"first_name", "last_name"}, QueryConditions.create(QueryCondition.greater("id", 3)).and(QueryCondition.like("email", "john%")), QueryOrder.by("first_name"), QueryLimit.limit(2));
```
Besides just passing a String as the column, you can pass any CharSequence, more specifically the [QueryFunction](#queryfunction).

In conclusion, the select method is any of the following:
```java
Database.select(String table, CharSequence column);
Database.select(String table, CharSequence column, QueryCondition condition);
Database.select(String table, CharSequence column, QueryCondition condition, QueryOrder order, QueryLimit limit);
Database.select(String table, CharSequence[] columns);
Database.select(String table, CharSequence[] columns, QueryCondition condition);
Database.select(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order, QueryLimit limit);
```

#### SelectBuilder
Because using all those select methods can be quite tricky and messy, there's also a `SelectBuilder`.  
As well as making selecting cleaner, SelectBuilder allows you to pass an alias for certain columns, e.g. selecting `count(*)` as `count`, 
and you can select into tables and variables.  

To create a new SelectBuilder, either use `Database#selectBuilder(String)` or `SelectBuilder#create(Database, String)`.  
To select a column, use `SelectBuilder#select(CharSequence)`, `SelectBuilder#select(CharSequence, String)`, `SelectBuilder#select(CharSequence...)` or `SelectBuilder#select(Iterable)`. The second parameter of `SelectBuilder#select(CharSequence, String)` is an optional alias.  
To select into a table or variable, use `SelectBuilder#into(String)`.  
To supply a condition, use `SelectBuilder#where(QueryCondition)`.  
To supply an order, use `SelectBuilder#order(QueryOrder)`, `SelectBuilder#order(String)` or `SelectBuilder#order(String, QueryOrder.QueryDirection)`.  
And last but not least, you can supply a limit using `SelectBuilder#limit(QueryLimit)`, `SelectBuilder#limit(int)` or `SelectBuilder#limit(int, int)`.  

Say you have a list of CharSequences you'd like to select from a table named `users` and you'd like to descendingly sort them by when they were created, but you only want 10 people from the 6th page and you only want people whose first name is John and whose last name starts with a D and you also wish to store the result in a table named `tempusers` and would like to select the `created_at` column as `time`, then you could use this example:  
```java
Database db = ...;
SelectResults res = db.selectBuilder("users")
    .select(columns)
    .select("created_at", "time")
    .into("tempusers")
    .where(QueryCondition.equals("first_name", "John").and(QueryCondition.like("last_name", "D%")))
    .order("time", QueryOrder.OrderDirection.DESC)
    .limit(10, 6 * 10)
    .execute();
```

### Inserting
#### New data
Inserting one value into one column is the easiest method. It can easily be done using
```java
db.insert("people", "first_name", "Tony");
```
Inserting one row, but with multiple columns is nearly as easy. For that you could use
```java
db.insert("people", new String[] {"first_name", "last_name", "email"}, new Object[] {"Tony", "Stark", "tonystark@mail.com"});
```
You can, however, also insert multiple rows at once. For this use
```java
db.insert("people", new String[] {"first_name", "last_name", "email"}, Lists.newArrayList(new Object[] {"Tony", "Stark", "tonystark@mail.com"}, new Object[] {"Thor", "Odinson", "thorodison@mail.com"}));
```
You must also use this method if you wish to insert multiple values for one column, just wrap the name of that column in a String array.

#### Update on duplicate
You also have the option to update columns of a row when a row with the same value for the PRIMARY KEY column already exists.
Say for example you have a table like people and id is the PRIMARY KEY, you could then overwrite the data that already exists or insert new data if it does not, for example:
```java
db.insertUpdate("people", new String[] {"id", "first_name", "last_name", "email"}, new Object[] {3, "Thor", "Odinson", "thorodinson@mail.com"}, ImmutableMap.<String, Object>builder().put("email", "thorodinson@mail.com").build());
```
This would try to insert a new row with the values `3`, `Thor`, `Odinson` and `thorodinson@mail.com` for columns `id`, `first_name`, `last_name` and `email` into the table, but because there is already a row with the value 3 for the PRIMARY KEY column (id), it insteads updates the value of column `email` of that row to 'thorodinson@gmail.com'.

#### Ignore on duplicate
Last but not least, instead of updating on a duplicate PRIMARY KEY value, you can also choose to ignore the insertion.  
An example of this would be
```java
db.insertIgnore("people", new String[] {"id", "first_name", "last_name", "email"}, new Object[] {3, "Thor", "Odinson", "thorodinson@mail.com"}, "id");
```
Where the last parameter 'id' is the name of the table's PRIMARY KEY column.  
This would end up doing nothing as there is already a row where column `id` has a value of 3.

#### InsertBuilder
Aside from [SelectBuilder](#selectbuilder), there's also an InsertBuilder. This one is a little less sophisticated, however.  
For the time being, you can only do normal insertions and replacements with this class.  
To create an InsertBuilder, either use `Database#insertBuilder(String, String...)` or use `InsertBuilder#create(Database, String, String...)`.  
The String parameter here is the table to insert into and the String varargs are the columns to insert into.  
To insert data, use `InsertBuilder#insert(Object...)`, `InsertBuilder#insert(Object[]... values)` or `InsertBuilder#insert(Iterable)`.  
Then use either `InsertBuilder#execute()` or `InsertBuilder#executeReplace()` to execute either an insert query or a replace query.  

### Updating
You can also choose to update data rather than inserting it.  
This can be done rather easily using
```java
db.update("people", ImmutableMap.<String, Object>builder().put("email", null).build(), QueryCondition.greater("id", 4));
```
This would set the `email` column to null for every row where `id` is greater than 4.

### Replacing
Besides inserting and updating, you can also replace rows.  
This is essentially the same as deleting any row with the same value for the PRIMARY KEY column and then inserting this one instead.  
The replace methods are the exact same as the insert methods as the queries look exactly the same, so for examples, have a look at inserting.

### Creating tables
Creating tables has not been made simpler, but it sure has been made more sane.  
Now creating tables can be done using the following example:
```java
TablePreset.create("users")
    .putColumn("id", ColumnType.BIGINT.struct()
        .configure(sup -> sup.apply(null))
        .setPrimary(true)
        .setNullAllowed(false))
    .putColumn("username", ColumnType.VARCHAR.struct()
        .configure(sup -> sup.apply(16))
        .setNullAllowed(false))
    .putColumn("password", ColumnType.CHAR.struct()
        .configure(sup -> sup.apply(128))
        .setNullAllowed(false))
    .create(db);
```
This preset would compile to `CREATE TABLE users (id BIGINT PRIMARY KEY NOT NULL, username VARCHAR(16) NOT NULL, password CHAR(128) NOT NULL);`.  
As you can see, it looks a little bit complicated at first sight, but it sure looks way prettier and leaves a lot less room for mistakes than just a plain `CREATE TABLE` query.  

For starters the `TablePreset#create(String)` method. This method creates a new TablePreset with the given name. This name can be changed later and if you plan on using the preset multiple times, it is recommended you do so using `preset.setName("newname").create(db)`.  

Next we have putting columns.  
Columns are gotten by running `ColumnType#struct()` on your desired ColumnType.  
If the selected ColumnType has a supplier that requires parameters, **it is mandatory that you configure it**.  
You can do so using `ColumnStructure#configure(Function)` where the function takes a supplier of sorts and returns its value after passing it your desired values.  
ColumnTypes can have suppliers for various reasons, but most often it requires one value which is the length or maximum length of the type.  
In any case, any values passed to the supplier is what appears in the parentheses behind it.  
Some, not all, suppliers also allow for being passed `null` values. Integers are an example of this.  

Last we have any attributes the column might have, this could be anything and I suggest you read the javadoc on classes in the `com.ptsmods.mysqlw.table` class to find out more.

### Database-backed collections
Next we have the real cool feature, database-backed collections.  
These collections are just ordinary lists, sets or maps, but their values are never present on the VM, at least not in a significant way.  
These classes were written with concurrent modifications and efficiency in mind. Two instances connected to the same database could edit the same list, set or map at the same time and there would not be an issue.  

All DbCollection classes require functions to convert their elements or keys and values to and from Strings in order to be stored.  
Basic functions for this and a registry for your own can be found in the DbCF (Database Collection Functions) class.

#### DbList
This list works with a table with two columns in the background. One for an id and one for a value.  
Since with lists the order matters and with MySQL auto-incrementing ids can be a bit unreliable (especially when adding a value somewhere in the list instead of at the end), whenever you remove a value or add one somewhere that's not at the end, the entire list is loaded, altered and inserted again.  
This can in some cases, especially with big lists, be problematic as it makes things slow, so keep that in mind.

#### DbSet
This set is just like any other set, except its values are stored in a table.  
The table consists of one column, that being a TEXT type.  
Just like your usual set, this set does not allow null or duplicate values and is more or less randomly ordered.

#### DbMap
A database-backed map, what else is there to say?  
Oh yeah, the table backing this map consists of two columns, one being the key with a type of VARCHAR(255), keep that maximum length in mind, the other being the value with a type of TEXT.  

#### Creating database-backed collections
Getting an instance of a DbList or a DbSet is more or less the same except for lists the method signature is `DbList#getList(Database, String, Class)` while for sets it's `DbSet#getSet(Database, String, Class)`, but you pass them the same parameters.  
Getting a new list is thus done as follows:
```java
List<String> list = DbList.getList(db, "testlist", String.class);
```
In this method, the parameters are in order: the database this list is on, the name of this list (used to determine the table name which is 'list_' + name) and the class of the type of this list.  
The last paremeter can be any class you like so long as it has type converters registered with `DbCF#registerConverters(Class, BiFunction, BiFunction)`.  
By default all basic Java types (String, Byte, Short, Integer, Long, Float and Double) are registered.  
You can, however, also nest DbLists, DbSets and DbMaps, to do so, you need to acquire their respective converters using the methods in the DbCF class.  
These methods are:
  - `dbListToStringFunc()`
  - `dbListFromStringFunc(Class)`
  - `dbListFromStringFunc(BiFunction, BiFunction)`
  - `dbSetToStringFunc()`
  - `dbSetFromStringFunc(Class)`
  - `dbSetFromStringFunc(BiFunction, BiFunction)`
  - `dbMapToStringFunc()`
  - `dbMapFromStringFunc(Class, Class)`
  - `dbMapFromStringFunc(BiFunction, BiFunction, BiFunction, BiFunction)`  
Note that the methods that require classes can only be used if those classes have registered type converters.  

Creating a DbMap is nearly the same as creating a DbList or DbSet, except you pass two types or two pairs of type converters.  
An example would be:
```
Map<String, Integer> map = DbMap.getMap(db, "testmap", String.class, Integer.class);
```

### Triggers and Procedures
Since 1.7, you can create triggers and procedures with MySQLw. These are created using the `BlockBuilder` class which has a method for every single statement supported by MySQLw and can be used to create most basic triggers and procedures. Any statement that's not supported can still be used with `BlockBuilder#raw(String)` or `RawStmt#raw(String)`, but if you rely on these, I suggest you [file an issue](https://github.com/PlanetTeamSpeakk/MySQLw/issues/new).  
You can create triggers using `Database#createTrigger(String, String, TriggeringEvent, BlockBuilder)` which creates a trigger with the given name on the given table.  
Procedures can be created with `Database#createProcedure(String, ProcedureParameter[], BlockBuilder)` and called with `Database#call(String, Object...)`.  
For an example on how to use `BlockBuilder` to create blocks, have a look at [the test I made for it](https://github.com/PlanetTeamSpeakk/MySQLw/blob/main/src/test/java/com/ptsmods/mysqlw/test/MySQLTest.java#L207).  

It should also be noted that you can create your own statements if you wish, you'll just have to use `BlockBuilder#stmt(Statement)` to actually add it to BlockBuilders.  
Any Statement implementing OpeningStatement will increase the indentation by one and any Statement implementing ClosingStatement will decrease it by one.

### Type conversion
By default, MySQLw can use a null value, any type of Number, String or byte array (it is converted to hex String representation) in queries, but to do so, it must first prepare them.  
This preparation is known as type conversion. To register your own type converter so you can put for example entire lists into a column, use the `Database#registerTypeConverter(Class, Function)` method.
An example of a type converter would be:
```java
Gson gson = new Gson();
Database.registerTypeConverter(List.class, list -> Database.enquote(gson.toJson(list)), s -> gson.fromJson(s, List.class));
```
This would allow you to use lists when inserting data by turning the list into a JSON representation of it and enquoting it.  
Using this newly added type converter is as simple as just passing it as a value when inserting, e.g.:
```java
db.insert("data", "value", Lists.newArrayList("This is a JSON list", "with numerous values", "How neat!"));
```
Which would put the string `'["This is a JSON list", "with numerous values", "How neat!"]'` into the query.  
Do not forget that enquoting is mandatory whenever you insert anything as a literal String into a table, not doing so will end up in SQLExceptions.

### Other smaller features
#### Counting
To count the amount of columns in a table, you can use the `Database#count(String, String, QueryCondition)` method.  
This is basically a select query, except it only returns the requested value of count.  
To use this, you could try the following code:
```java
int count = db.count("people", "*", QueryCondition.like("email", "john%"));
```
This will return the amount of rows in the table `people` where the email starts with 'john'.
In case of the previous table, this would be 2.

#### Truncate
To completely wipe a table (truncating), you can use
```java
db.truncate("people");
```
This will clear the whole table and also reset the starting value of the id column assuming it has the AUTO_INCREMENT attribute so any new entries will start at 1.

#### Delete
Deleting data is also made easy as that is the entire purpose of this library. It can be done using the following code:
```java
int affected = db.delete("people", QueryCondtions.create(QueryCondition.equals("first_name", "Harry")).or(QueryCondition.equals("first_name", "Clark")));
```
This would remove Harry Potter and Clark Kent from the table and return 2 as that is the amount of rows it affected.

#### QueryFunction
This class is used to put a raw String into a query. This is especially useful when you want to check against JSON columns or any other context where you may want to use any sort of function (count is also an example).  
To use a `QueryFunction` in a select query, you could use the following:
```java
SelectResults results = db.select("people", new QueryFunction("count(*)"), null, null);
```
This would return a `SelectResults` object with its only value being the result of the count function. (Although in this case, you could also use the `Database#count(String, String, QueryCondition)` method.)

#### Enquoting
Enquoting is as simple as putting a String in between single quotes and escaping all single quotes it contains by replacing them with two single quotes.  
Enquoting can be done using
```java
String s = Database.enquote("Test'S'tring");
```
This would return `'Test''S''tring` which can safely be used to insert directly into a query.  
If you wish to just escape quotes and don't put the string in two single quotes in a whole, you can use
```java
String s = Database.escapeQuotes("Test'S'tring");
```
This would return `Test''S''tring` which is the same value as with enquoting, except not put in quotes.

#### Dropping
To completely delete a table (dropping), you can use the following code:
```java
db.drop("people");
```

## Async
Nearly every method that uses the database connection in one way or another has an async version that uses `CompletableFuture`s to run the method asynchronously.  
For example, to insert data asynchronously, you can do the following:
```java
db.selectAsync("people", "*").thenAccept(results -> {
	// Do something with the results.
});
```
This will prevent blocking the main thread or whatever thread you wish to run it on.  
By default, these `CompletableFuture`s use an executor that suits the RDBMS type. This is a normal cached threadpool for MySQL (see `Executors#newCachedThreadPool(ThreadFactory)`) and a fixed-size threadpool that only allows one thread for SQLite. The latter is to prevent blocking.  
This executor can be gotten using `Database#getExecutor()` and set using `Database#setExecutor(executor)`.  
With asynchronous calls always comes the struggle of correctly catching exceptions, for this reason you can set your own errorhandler using `Database#setErrorHandler(Consumer)`, this consumer will then be called whenever an error was thrown during any asynchronous database call.
