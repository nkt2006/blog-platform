package ru.mirea.blogplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.exception.DatabaseException;
import ru.mirea.blogplatform.model.*;
import ru.mirea.blogplatform.repository.jdbc.*;

/** Проверяет тот же Service → JDBC маршрут, который вызывают JavaFX-контроллеры. */
@EnabledIfEnvironmentVariable(named = "BLOG_TEST_DB_URL", matches = ".+")
class DesktopWorkflowIntegrationTest {
    @TempDir Path output;

    @Test void servicesPersistCrudModerationFilteredExportAndCascade() throws Exception {
        DatabaseManager database = new DatabaseManager(System.getenv("BLOG_TEST_DB_URL"),
                System.getenv().getOrDefault("BLOG_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("BLOG_TEST_DB_PASSWORD", ""));
        JdbcUserRepository userRepository = new JdbcUserRepository(database);
        JdbcBlogPostRepository postRepository = new JdbcBlogPostRepository(database);
        JdbcCommentRepository commentRepository = new JdbcCommentRepository(database);
        UserService users = new UserService(userRepository);
        BlogPostService posts = new BlogPostService(postRepository, userRepository, commentRepository);
        CommentService comments = new CommentService(commentRepository, postRepository, userRepository);
        User author = users.create("JavaFX-тест", "desktop-" + System.nanoTime() + "@example.com", UserRole.AUTHOR);
        Long postId = null;
        try {
            users.update(author.getId(), "Автор JavaFX", author.getEmail(), UserRole.EDITOR);
            assertEquals(UserRole.EDITOR, users.getById(author.getId()).getRole());
            BlogPost post = posts.createPost(author.getId(), "Демонстрация", "desktop-" + System.nanoTime(), "Первый текст");
            postId = post.getId();
            posts.updatePost(post.getId(), author.getId(), "Изменённая публикация", post.getSlug(), "Новый текст");
            assertThrows(BusinessRuleException.class, () -> posts.changeStatus(post.getId(), PostStatus.PUBLISHED));
            posts.changeStatus(post.getId(), PostStatus.REVIEW);
            posts.changeStatus(post.getId(), PostStatus.PUBLISHED);
            assertNotNull(posts.getPostById(post.getId()).getPublishedAt());
            Comment comment = comments.create(post.getId(), author.getId(), "Первый комментарий");
            comments.update(comment.getId(), "Исправленный комментарий");
            comments.changeStatus(comment.getId(), CommentStatus.APPROVED);
            assertEquals(CommentStatus.APPROVED, comments.getById(comment.getId()).getStatus());
            assertThrows(DatabaseException.class, () -> users.delete(author.getId()));

            WorkspaceSnapshot data = new WorkspaceSnapshot(users.list(), posts.listPosts(), comments.list());
            CatalogQueryService queries = new CatalogQueryService();
            List<BlogPost> visible = data.posts().stream().filter(item -> queries.matchesPost(item, data,
                    "Изменённая", PostStatus.PUBLISHED, author, null, null)).toList();
            assertEquals(1, visible.size());
            TableExportService exporter = new TableExportService();
            Path excel = exporter.export(exporter.posts(visible, data), output.resolve("filtered.xlsx"));
            try (var stream = Files.newInputStream(excel); var book = new XSSFWorkbook(stream)) {
                assertEquals(1, book.getNumberOfSheets());
                assertEquals(1, book.getSheetAt(0).getLastRowNum());
                assertEquals("Изменённая публикация", book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            }
            Path csv = exporter.export(exporter.comments(List.of(comments.getById(comment.getId())), data),
                    output.resolve("comments.csv"));
            assertTrue(Files.readString(csv).contains("Исправленный комментарий"));
            assertTrue(new StatisticsService().calculate(data).getTotalPosts() >= 1);
            posts.deletePost(post.getId());
            postId = null;
            assertTrue(commentRepository.findById(comment.getId()).isEmpty());
        } finally {
            if (postId != null) postRepository.deleteById(postId);
            userRepository.deleteById(author.getId());
        }
        assertTrue(userRepository.findById(author.getId()).isEmpty());
    }
}
