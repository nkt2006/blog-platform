package ru.mirea.blogplatform.exception;

/** Signals that application data could not be written to an export file. */
public class DataExportException extends RuntimeException {
    public DataExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
