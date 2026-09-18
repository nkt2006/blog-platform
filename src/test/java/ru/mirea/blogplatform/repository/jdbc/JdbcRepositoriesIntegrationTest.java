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
import ru.mirea.blogplatform.model.PostStatus;

@EnabledIfEnvironmentVariable(named = "BLOG_TEST_DB_URL", matches = ".+")
class JdbcRepositoriesIntegrationTest {
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
