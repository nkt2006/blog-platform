package ru.mirea.blogplatform.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ru.mirea.blogplatform.exception.DatabaseException;

/** Creates PostgreSQL connections using environment variables or sensible local defaults. */
public final class DatabaseManager {
    public static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/blog_platform";
    public static final String DEFAULT_USER = "postgres";
    public static final String DEFAULT_PASSWORD = "postgres";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        this(
                valueOrDefault(System.getenv("BLOG_DB_URL"), DEFAULT_URL),
                valueOrDefault(System.getenv("BLOG_DB_USER"), DEFAULT_USER),
                valueOrDefault(System.getenv("BLOG_DB_PASSWORD"), DEFAULT_PASSWORD)
        );
    }

    public DatabaseManager(String url, String user, String password) {
        this.url = requireValue(url, "Адрес базы данных");
        this.user = requireValue(user, "Пользователь базы данных");
        this.password = password == null ? "" : password;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось подключиться к базе данных", exception);
        }
    }

    public String getUrl() {
        return url;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым.");
        }
        return value;
    }
}
