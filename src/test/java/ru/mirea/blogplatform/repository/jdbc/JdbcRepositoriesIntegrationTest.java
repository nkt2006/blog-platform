package ru.mirea.blogplatform.repository.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;

@EnabledIfEnvironmentVariable(named = "BLOG_TEST_DB_URL", matches = ".+")
class JdbcRepositoriesIntegrationTest {
    @Test
    void performsUserAndCommentCrudAgainstPostgres() {
        DatabaseManager databaseManager = new DatabaseManager(
                System.getenv("BLOG_TEST_DB_URL"),
                System.getenv().getOrDefault("BLOG_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("BLOG_TEST_DB_PASSWORD", "")
        );
        JdbcUserRepository users = new JdbcUserRepository(databaseManager);
        JdbcBlogPostRepository posts = new JdbcBlogPostRepository(databaseManager);
        JdbcCommentRepository comments = new JdbcCommentRepository(databaseManager);

        User author = users.create(new User(null, "Тестовый автор",
                "integration-" + System.nanoTime() + "@example.com", UserRole.AUTHOR));
        BlogPost post = null;
        Comment comment = null;
        try {
            assertNotNull(author.getId());
            assertTrue(users.existsByEmailAndIdNot(author.getEmail(), -1));
            author.setRole(UserRole.EDITOR);
            assertTrue(users.update(author));
            assertEquals(UserRole.EDITOR, users.findById(author.getId()).orElseThrow().getRole());

            post = posts.create(new BlogPost(null, author.getId(), "Тестовый пост",
                    "comment-integration-" + System.nanoTime(), "Содержание",
                    PostStatus.DRAFT, null, null));
            comment = comments.create(new Comment(null, post.getId(), author.getId(),
                    "Тестовый комментарий", CommentStatus.PENDING, LocalDateTime.now()));
            assertNotNull(comment.getId());
            long commentId = comment.getId();
            assertEquals("Тестовый комментарий", comments.findById(comment.getId()).orElseThrow().getText());
            assertTrue(comments.findAll().stream().anyMatch(item -> item.getId() == commentId));

            comment.setText("Обновлённый комментарий");
            comment.setStatus(CommentStatus.APPROVED);
            assertTrue(comments.update(comment));
            assertEquals(CommentStatus.APPROVED,
                    comments.findById(comment.getId()).orElseThrow().getStatus());
            assertTrue(comments.deleteById(comment.getId()));
            assertFalse(comments.findById(comment.getId()).isPresent());
            comment = null;
        } finally {
            if (comment != null) {
                comments.deleteById(comment.getId());
            }
            if (post != null) {
                posts.deleteById(post.getId());
            }
            users.deleteById(author.getId());
        }
        assertFalse(users.findById(author.getId()).isPresent());
    }

    @Test
    void performsCrudAndSlugChecksAgainstPostgres() {
        DatabaseManager databaseManager = new DatabaseManager(
                System.getenv("BLOG_TEST_DB_URL"),
                System.getenv().getOrDefault("BLOG_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("BLOG_TEST_DB_PASSWORD", "")
        );
        JdbcUserRepository userRepository = new JdbcUserRepository(databaseManager);
        JdbcBlogPostRepository postRepository = new JdbcBlogPostRepository(databaseManager);

        long authorId = userRepository.findAll().get(0).getId();
        String slug = "jdbc-integration-" + System.nanoTime();
        BlogPost post = new BlogPost(
                null,
                authorId,
                "Интеграционный тест",
                slug,
                "Проверка JDBC-репозитория",
                PostStatus.DRAFT,
                null,
                null
        );

        BlogPost created = postRepository.create(post);
        assertNotNull(created.getId());
        assertTrue(postRepository.existsBySlug(slug));
        assertEquals(slug, postRepository.findById(created.getId()).orElseThrow().getSlug());

        created.setTitle("Обновлённая публикация");
        created.setStatus(PostStatus.PUBLISHED);
        created.setPublishedAt(LocalDateTime.now());
        assertTrue(postRepository.update(created));
        assertEquals(
                PostStatus.PUBLISHED,
                postRepository.findById(created.getId()).orElseThrow().getStatus()
        );
        assertFalse(postRepository.existsBySlugAndIdNot(slug, created.getId()));

        assertTrue(postRepository.deleteById(created.getId()));
        assertFalse(postRepository.findById(created.getId()).isPresent());
        assertFalse(postRepository.deleteById(created.getId()));
    }
}
