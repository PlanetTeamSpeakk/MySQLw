package com.ptsmods.mysqlw.procedure;

/**
 * A simple interface that should only ever be implemented by the BlockBuilder class.<br>
 * Used to be able to move procedures to their own module.
 */
public interface IBlockBuilder {

    IBlockBuilder wrapForProcedure(String delimiter);
}
