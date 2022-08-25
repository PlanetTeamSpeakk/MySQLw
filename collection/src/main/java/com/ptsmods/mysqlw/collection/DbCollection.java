package com.ptsmods.mysqlw.collection;

import com.ptsmods.mysqlw.Database;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.function.BiFunction;

/**
 * An interface implemented by {@link DbList}, {@link DbSet} and {@link DbMap} which always pass themselves as an instance of this interface
 * when converting their key/value or element type to/from a String.
 * @see DbCF
 */
public interface DbCollection {

    /**
     * @return The database this collection works with.
     */
    Database getDb();

    /**
     * @return The table this collection works with.
     */
    String getTable();

    /**
     * @return The name of this collection. Used to cache and parse from Strings.
     */
    String getName();

}
