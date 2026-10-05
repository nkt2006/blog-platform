package ru.mirea.blogplatform.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.exception.DatabaseException;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;
import ru.mirea.blogplatform.repository.UserRepository;

public class JdbcUserRepository implements UserRepository {
    private static final String CREATE_SQL =
            "INSERT INTO users (name, email, role) VALUES (?, ?, ?)";
    private static final String FIND_BY_ID_SQL = """
            SELECT id, name, email, role
            FROM users
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, name, email, role
            FROM users
            ORDER BY id
            """;
    private static final String UPDATE_SQL =
            "UPDATE users SET name = ?, email = ?, role = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM users WHERE id = ?";
    private static final String EMAIL_EXISTS_SQL =
            "SELECT EXISTS(SELECT 1 FROM users WHERE email = ? AND id <> ?)";

    private final DatabaseManager databaseManager;

    public JdbcUserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public User create(User user) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     CREATE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setFields(statement, user);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула ID автора");
                }
                user.setId(keys.getLong(1));
                return user;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось создать автора", exception);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось получить автора с ID " + id, exception);
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось получить список авторов", exception);
        }
    }

    @Override
    public boolean update(User user) {
        if (user.getId() == null) {
            return false;
        }
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            setFields(statement, user);
            statement.setLong(4, user.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось обновить автора с ID " + user.getId(), exception);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Не удалось удалить автора с ID " + id + ". Возможно, у него есть публикации или комментарии.",
                    exception);
        }
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, long excludedId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(EMAIL_EXISTS_SQL)) {
            statement.setString(1, email);
            statement.setLong(2, excludedId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось проверить email автора", exception);
        }
    }

    private void setFields(PreparedStatement statement, User user) throws SQLException {
        statement.setString(1, user.getName());
        statement.setString(2, user.getEmail());
        statement.setString(3, user.getRole().name());
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                UserRole.valueOf(resultSet.getString("role"))
        );
    }
}
