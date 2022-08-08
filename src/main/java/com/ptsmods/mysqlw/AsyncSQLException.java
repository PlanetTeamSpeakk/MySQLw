package com.ptsmods.mysqlw;

import java.util.concurrent.CompletionException;

public class AsyncSQLException extends Exception {
    private final StackTraceElement[] stacktrace;
    private final Throwable rootTrace;
    private final Throwable exception;

    public AsyncSQLException(Throwable t, Exception rootTrace) {
        if (t instanceof CompletionException) t = t.getCause();
        stacktrace = t.getStackTrace();
        this.rootTrace = rootTrace;
        exception = t;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return stacktrace;
    }

    /**
     * This is only meant for the automatic stacktrace printing by #printStackTrace() and loggers.
     * For the actual exception that caused all this, use {@link #getException()}.
     * @return The tracing exception leading to the method that made the async call.
     */
    @Deprecated
    @Override
    public Throwable getCause() {
        return rootTrace;
    }

    /**
     * @return the original exception this exception was wrapped around.
     */
    public Throwable getException() {
        return exception;
    }
}
