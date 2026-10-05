package ru.mirea.blogplatform.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.CommentRepository;
import ru.mirea.blogplatform.repository.UserRepository;

class ExcelExporterTest {
    @TempDir
    Path tempDirectory;

    @Test
    void exportsAllThreeEntities() throws IOException {
        User author = new User(1L, "Автор", "author@example.com");
        BlogPost post = new BlogPost(1L, 1L, "Пост", "post", "Текст",
                PostStatus.DRAFT, LocalDateTime.of(2026, 10, 5, 12, 0), null);
        Comment comment = new Comment(1L, 1L, 1L, "Комментарий",
                CommentStatus.APPROVED, LocalDateTime.of(2026, 10, 5, 12, 1));
        ExcelExporter exporter = new ExcelExporter(new StubUserRepository(List.of(author)),
                new StubBlogPostRepository(List.of(post)), new StubCommentRepository(List.of(comment)));

        Path output = exporter.export(tempDirectory.resolve("all-entities.xlsx"));
        try (InputStream input = Files.newInputStream(output);
             Workbook workbook = new XSSFWorkbook(input)) {
            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals("Комментарии", workbook.getSheetAt(2).getSheetName());
            assertEquals("Комментарий",
                    workbook.getSheet("Комментарии").getRow(1).getCell(3).getStringCellValue());
            assertEquals("Одобрен",
                    workbook.getSheet("Комментарии").getRow(1).getCell(4).getStringCellValue());
        }
    }

    @Test
    void exportsAuthorsAndPostsToReadableWorkbook() throws IOException {
        User author = new User(1L, "Автор", "author@example.com");
        BlogPost post = new BlogPost(
                10L,
                1L,
                "Первая публикация",
                "first-post",
                "Текст публикации",
                PostStatus.PUBLISHED,
                LocalDateTime.of(2026, 9, 1, 12, 0),
                LocalDateTime.of(2026, 9, 2, 12, 0)
        );

        ExcelExporter exporter = new ExcelExporter(
                new StubUserRepository(List.of(author)),
                new StubBlogPostRepository(List.of(post))
        );

        Path output = exporter.export(tempDirectory.resolve("nested/blog-data.xlsx"));

        assertTrue(Files.exists(output));
        try (InputStream inputStream = Files.newInputStream(output);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Авторы", workbook.getSheetAt(0).getSheetName());
            assertEquals("Публикации", workbook.getSheetAt(1).getSheetName());
            assertEquals("Автор", workbook.getSheet("Авторы").getRow(1).getCell(1).getStringCellValue());
            assertEquals(
                    "Адрес публикации",
                    workbook.getSheet("Публикации").getRow(0).getCell(3).getStringCellValue()
            );
            assertEquals(
                    "first-post",
                    workbook.getSheet("Публикации").getRow(1).getCell(3).getStringCellValue()
            );
            assertEquals(
                    "Опубликована",
                    workbook.getSheet("Публикации").getRow(1).getCell(4).getStringCellValue()
            );
        }
    }

    private record StubUserRepository(List<User> users) implements UserRepository {
        @Override
        public User create(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> findById(long id) {
            return users.stream().filter(user -> user.getId() == id).findFirst();
        }

        @Override
        public List<User> findAll() {
            return users;
        }

        @Override
        public boolean update(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByEmailAndIdNot(String email, long excludedId) {
            return users.stream().anyMatch(user -> user.getId() != excludedId
                    && user.getEmail().equalsIgnoreCase(email));
        }
    }

    private record StubBlogPostRepository(List<BlogPost> posts) implements BlogPostRepository {
        @Override
        public BlogPost create(BlogPost post) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<BlogPost> findById(long id) {
            return posts.stream().filter(post -> post.getId() == id).findFirst();
        }

        @Override
        public List<BlogPost> findAll() {
            return posts;
        }

        @Override
        public boolean update(BlogPost post) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsBySlug(String slug) {
            return posts.stream().anyMatch(post -> post.getSlug().equals(slug));
        }

        @Override
        public boolean existsBySlugAndIdNot(String slug, long excludedId) {
            return posts.stream().anyMatch(
                    post -> post.getSlug().equals(slug) && post.getId() != excludedId
            );
        }
    }

    private record StubCommentRepository(List<Comment> comments) implements CommentRepository {
        @Override
        public Comment create(Comment comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Comment> findById(long id) {
            return comments.stream().filter(comment -> comment.getId() == id).findFirst();
        }

        @Override
        public List<Comment> findAll() {
            return comments;
        }

        @Override
        public boolean update(Comment comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(long id) {
            throw new UnsupportedOperationException();
        }
    }
}
