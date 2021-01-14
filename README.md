# MySQLw

[![Latest release](https://img.shields.io/github/release/PlanetTeamSpeakk/MySQLw.svg)](https://github.com/PlanetTeamSpeakk/MySQLw/releases/latest)
[![Build Status](https://travis-ci.org/PlanetTeamSpeakk/MySQLw.svg?branch=master)](https://travis-ci.org/PlanetTeamSpeakk/MySQLw)

A wrapper for MySQL connections for Java to make your life a lot easier and a lot saner when working with queries.  
This library is merely a wrapper for the default Java SQL library intended to be used with MySQL (or MariaDB for that matter), this means that you also need the MySQL Java connector to actually connect to your database.

* [Adding MySQLw to your project](#adding-mysqlw-to-your-project)
  + [Gradle](#gradle)
  + [Maven](#maven)
* [Usage](#usage)
  + [Connecting](#connecting)
  + [Selecting](#selecting)
  + [Inserting](#inserting)
    - [New data](#new-data)
    - [Update on duplicate](#update-on-duplicate)
    - [Ignore on duplicate](#ignore-on-duplicate)
  + [Updating](#updating)
  + [Replacing](#replacing)
  + [Creating tables](#creating-tables)
  + [Database-backed collections](#database-backed-collections)
    - [DbList](#dblist)
    - [DbSet](#dbset)
    - [DbMap](#dbmap)
  + [Type conversion](#type-conversion)
  + [Other smaller features](#other-smaller-features)
    - [Counting](#counting)
    - [Truncate](#truncate)
    - [Delete](#delete)
    - [QueryFunction](#queryfunction)
    - [Enquoting](#enquoting)
    - [Dropping](#dropping)

## Adding MySQLw to your project
### Gradle
To add MySQLw to your Gradle project, add the following line to your dependencies:
```gradle
compile 'com.ptsmods:MySQLw:1.0'
```

### Maven
To add MySQLw to your Maven project, add the following code segment to your pom.xml file:
```xml
<dependency>
  <groupId>com.ptsmods</groupId>
  <artifactId>MySQLw</artifactId>
  <version>1.0</version>
</dependency>
```

## Usage
### Connecting
To start off, you should connect to your database. Assuming the MySQL Java connector is on your classpath too, you can do so with the following code:
```java
String host = "localhost";
int port = 3306;
String name = "mydatabase";
String username = "root";
String password = null;
Database db = Database.connect(host, port, name, username, password);
```

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
SelectResults results = db.select("people", "last_name", null, null);
```
Now if you wish to do the same, but only if the id is greater than 3, you could use
```java
SelectResults results = db.select("people", "last_name", QueryCondition.greater("id", 3), null);
```
Now if you wish to only return results whose email address starts with 'john' and order by first_name and you also want the first_name column returned, you can do so with
```java
SelectResults results = db.select("people", new String[] {"first_name", "last_name"}, QueryConditions.create(QueryCondition.greater("id", 3)).and(QueryCondition.like("email", "john%")), QueryOrder.by("first_name"));
```
Besides just passing a String as the column, you can pass any CharSequence, more specifically the QueryFunction.

In conclusion, the select method is either one of the following:
```java
Database.select(String table, CharSequence column, QueryCondition condition, QueryOrder order);
Database.select(String table, CharSequence[] columns, QueryCondition condition, QueryOrder order);
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
    .putColumn("id", ColumnType.BIGINT.createStructure()
        .satiateSupplier(sup -> sup.apply(null))
        .setPrimary(true)
        .setNullAllowed(false))
    .putColumn("username", ColumnType.VARCHAR.createStructure()
        .satiateSupplier(sup -> sup.apply(16))
        .setNullAllowed(false))
    .putColumn("password", ColumnType.CHAR.createStructure()
        .satiateSupplier(sup -> sup.apply(128))
        .setNullAllowed(false))
    .create(db);
```
This preset would compile to `CREATE TABLE users (id BIGINT PRIMARY KEY NOT NULL, username VARCHAR(16) NOT NULL, password CHAR(128) NOT NULL);`.  
As you can see, it looks a little bit complicated at first sight, but it sure looks way prettier and leaves a lot less room for mistakes than just a plain `CREATE TABLE` query.  

For starters the `TablePreset#create(String)` method. This method creates a new TablePreset with the given name. This name can be changed later and if you plan on using the preset multiple times, it is recommended you do so using `preset.setName("newname").create(db)`.  

Next we have putting columns.  
Columns are gotten by running `ColumnType#createStructure()` on your desired ColumnType.  
If the selected ColumnType has a supplier that requires parameters, **it is mandatory that you satiate it**.  
You can do so using `ColumnStructure#satiateSupplier(Function)` where the function takes a supplier of sorts and returns its value after passing it your desired values.  
ColumnTypes can have suppliers for various reasons, but most often it requires one value which is the length or maximum length of the type.  
In any case, any values passed to the supplier is what appears in the parentheses behind it.  
Some, not all, suppliers also allow for being passed `null` values. Integers are an example of this.  

Last we have any attributes the column might have, this could be anything and I suggest you read the javadoc on classes in the `com.ptsmods.mysqlw.table` class to find out more.

### Database-backed collections
Next we have the real cool feature, database-backed collections.  
These collections are just ordinary lists, sets or maps, but their values are never present on the VM, at least not in a significant way.  
These classes were written with concurrent modifications and efficiency in mind. Two instances connected to the same database could edit the same list, set or map at the same time and there would not be an issue.  
All DbCollection classes require functions to convert their elements or keys and values to and from Strings in order to be stored.

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
Oh yeah, the table backing this map consists of two columns, on being the key with a type of VARCHAR(255), keep that maximum length in mind, the other being the value with a type of TEXT.  

### Type conversion
By default, MySQLw can use a null value, any type of Number, String or byte array (in hex String representation) in queries, but to do so, it must first prepare them.  
This preparation is known as type conversion. To register your own type converter so you can put for example entire lists into a column, use the `Database#registerTypeConverter(Class, Function)` method.
An example of a type converter would be:
```java
Gson gson = new Gson();
Database.registerTypeConverter(List.class, list -> Database.enquote(gson.toJson(list)));
```
This would allow you to use lists when inserting data by turning the list into a JSON representation of it and enquoting it.  
Using this newly added type converter is as simple as just passing it as a value when inserting, e.g.:
```java
db.insert("data", "value", Lists.newArrayList("This is a JSON list", "with numerous values", "How neat!"));
```
Which would put the string `'["This is a JSON list", "with numerous values", "How neat!"]'` into the query.

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
