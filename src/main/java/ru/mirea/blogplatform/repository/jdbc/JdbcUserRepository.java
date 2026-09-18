package ru.mirea.blogplatform.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.exception.DatabaseException;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.repository.UserRepository;

public class JdbcUserRepository implements UserRepository {
    private static final String FIND_BY_ID_SQL = """
            SELECT id, name, email
            FROM users
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, name, email
            FROM users
            ORDER BY id
            """;

    private final DatabaseManager databaseManager;

    public JdbcUserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
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

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("email")
        );
    }
}
