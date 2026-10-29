package ru.mirea.blogplatform.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import ru.mirea.blogplatform.exception.DatabaseException;

/** Creates PostgreSQL connections using environment variables or sensible local defaults. */
public final class DatabaseManager {
    public static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/blog_platform";
    public static final String DEFAULT_USER = "postgres";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        this(loadSettings());
    }

    private DatabaseManager(Properties settings) {
        this(valueOrDefault(System.getenv("BLOG_DB_URL"), settings.getProperty("db.url", DEFAULT_URL)),
                valueOrDefault(System.getenv("BLOG_DB_USER"), settings.getProperty("db.user", DEFAULT_USER)),
                System.getenv().getOrDefault("BLOG_DB_PASSWORD", settings.getProperty("db.password", "")));
    }

    public DatabaseManager(String url, String user, String password) {
        this.url = requireValue(url, "Адрес базы данных");
        this.user = requireValue(user, "Пользователь базы данных");
        this.password = password == null ? "" : password;
    }

    public Connection getConnection() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", password);
            properties.setProperty("connectTimeout", "5");
            properties.setProperty("socketTimeout", "15");
            return DriverManager.getConnection(url, properties);
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось подключиться к PostgreSQL. Проверьте, что база запущена, "
                    + "а адрес, пользователь и пароль в database.properties или BLOG_DB_* указаны верно.", exception);
        }
    }

    public String getUrl() {
        return url;
    }

    private static Properties loadSettings() {
        Properties settings = new Properties();
        try (InputStream resource = DatabaseManager.class.getResourceAsStream("/database.properties")) {
            if (resource != null) {
                settings.load(resource);
            }
            String configured = System.getProperty("blog.db.config", System.getenv("BLOG_DB_CONFIG"));
            Path local = Path.of(configured == null ? "database.local.properties" : configured);
            if (configured != null || Files.exists(local)) {
                try (InputStream stream = Files.newInputStream(local)) {
                    settings.load(stream);
                }
            }
            return settings;
        } catch (IOException exception) {
            throw new DatabaseException("Не удалось прочитать настройки базы данных.", exception);
        }
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
