package ru.mirea.blogplatform.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.exception.DatabaseException;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.repository.CommentRepository;

public class JdbcCommentRepository implements CommentRepository {
    private static final String CREATE_SQL = """
            INSERT INTO comments (post_id, author_id, text, status, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SELECT_SQL = """
            SELECT id, post_id, author_id, text, status, created_at
            FROM comments
            """;
    private static final String UPDATE_SQL = """
            UPDATE comments SET text = ?, status = ? WHERE id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM comments WHERE id = ?";

    private final DatabaseManager databaseManager;

    public JdbcCommentRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Comment create(Comment comment) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     CREATE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, comment.getPostId());
            statement.setLong(2, comment.getAuthorId());
            statement.setString(3, comment.getText());
            statement.setString(4, comment.getStatus().name());
            statement.setTimestamp(5, Timestamp.valueOf(comment.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула ID комментария");
                }
                comment.setId(keys.getLong(1));
                return comment;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось создать комментарий", exception);
        }
    }

    @Override
    public Optional<Comment> findById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL + "WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось получить комментарий с ID " + id, exception);
        }
    }

    @Override
    public List<Comment> findAll() {
        List<Comment> comments = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SQL + "ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                comments.add(map(result));
            }
            return comments;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось получить комментарии", exception);
        }
    }

    @Override
    public boolean update(Comment comment) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, comment.getText());
            statement.setString(2, comment.getStatus().name());
            statement.setLong(3, comment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось обновить комментарий с ID " + comment.getId(), exception);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось удалить комментарий с ID " + id, exception);
        }
    }

    private Comment map(ResultSet result) throws SQLException {
        return new Comment(
                result.getLong("id"),
                result.getLong("post_id"),
                result.getLong("author_id"),
                result.getString("text"),
                CommentStatus.valueOf(result.getString("status")),
                result.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
