package com.ptsmods.mysqlw.procedure.stmt.block;

/**
 * Statements implementing this interface output multi-statement blocks.
 * To ensure these are indented correctly, a depth may be passed.
 */
public interface BlockLikeStatement {
    String toString(int depth);
}
