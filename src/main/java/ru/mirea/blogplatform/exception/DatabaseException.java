package ru.mirea.blogplatform.exception;

/** Wraps technical JDBC failures without exposing checked SQL exceptions to the service layer. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
