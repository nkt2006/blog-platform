package ru.mirea.blogplatform.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.exception.DatabaseException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.repository.BlogPostRepository;

public class JdbcBlogPostRepository implements BlogPostRepository {
    private static final String CREATE_SQL = """
            INSERT INTO blog_posts
                (author_id, title, slug, content, status, created_at, published_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, author_id, title, slug, content, status, created_at, published_at
            FROM blog_posts
            WHERE id = ?
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, author_id, title, slug, content, status, created_at, published_at
            FROM blog_posts
            ORDER BY id
            """;

    private static final String UPDATE_SQL = """
            UPDATE blog_posts
            SET author_id = ?, title = ?, slug = ?, content = ?, status = ?,
                created_at = ?, published_at = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM blog_posts WHERE id = ?";
    private static final String EXISTS_BY_SLUG_SQL =
            "SELECT EXISTS(SELECT 1 FROM blog_posts WHERE slug = ?)";
    private static final String EXISTS_BY_SLUG_AND_ID_NOT_SQL =
            "SELECT EXISTS(SELECT 1 FROM blog_posts WHERE slug = ? AND id <> ?)";

    private final DatabaseManager databaseManager;

    public JdbcBlogPostRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public BlogPost create(BlogPost post) {
        LocalDateTime createdAt = post.getCreatedAt() == null
                ? LocalDateTime.now()
                : post.getCreatedAt();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     CREATE_SQL,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            setPostFields(statement, post, createdAt);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new DatabaseException("База данных не вернула ID созданной публикации");
                }
                post.setId(generatedKeys.getLong(1));
                post.setCreatedAt(createdAt);
                return post;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось создать публикацию", exception);
        }
    }

    @Override
    public Optional<BlogPost> findById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapPost(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось получить публикацию с ID " + id, exception);
        }
    }

    @Override
    public List<BlogPost> findAll() {
        List<BlogPost> posts = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                posts.add(mapPost(resultSet));
            }
            return posts;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось получить список публикаций", exception);
        }
    }

    @Override
    public boolean update(BlogPost post) {
        if (post.getId() == null) {
            return false;
        }

        LocalDateTime createdAt = post.getCreatedAt() == null
                ? LocalDateTime.now()
                : post.getCreatedAt();

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            setPostFields(statement, post, createdAt);
            statement.setLong(8, post.getId());
            boolean updated = statement.executeUpdate() > 0;
            if (updated) {
                post.setCreatedAt(createdAt);
            }
            return updated;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось обновить публикацию с ID " + post.getId(), exception);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось удалить публикацию с ID " + id, exception);
        }
    }

    @Override
    public boolean existsBySlug(String slug) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(EXISTS_BY_SLUG_SQL)) {
            statement.setString(1, slug);
            return readExists(statement);
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось проверить уникальность slug", exception);
        }
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, long excludedId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(EXISTS_BY_SLUG_AND_ID_NOT_SQL)) {
            statement.setString(1, slug);
            statement.setLong(2, excludedId);
            return readExists(statement);
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось проверить уникальность slug", exception);
        }
    }

    private void setPostFields(
            PreparedStatement statement,
            BlogPost post,
            LocalDateTime createdAt
    ) throws SQLException {
        statement.setLong(1, post.getAuthorId());
        statement.setString(2, post.getTitle());
        statement.setString(3, post.getSlug());
        statement.setString(4, post.getContent());
        statement.setString(5, post.getStatus().name());
        statement.setTimestamp(6, Timestamp.valueOf(createdAt));
        setNullableTimestamp(statement, 7, post.getPublishedAt());
    }

    private void setNullableTimestamp(
            PreparedStatement statement,
            int parameterIndex,
            LocalDateTime value
    ) throws SQLException {
        if (value == null) {
            statement.setTimestamp(parameterIndex, null);
        } else {
            statement.setTimestamp(parameterIndex, Timestamp.valueOf(value));
        }
    }

    private boolean readExists(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private BlogPost mapPost(ResultSet resultSet) throws SQLException {
        Timestamp publishedAt = resultSet.getTimestamp("published_at");
        return new BlogPost(
                resultSet.getLong("id"),
                resultSet.getLong("author_id"),
                resultSet.getString("title"),
                resultSet.getString("slug"),
                resultSet.getString("content"),
                PostStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                publishedAt == null ? null : publishedAt.toLocalDateTime()
        );
    }
}
