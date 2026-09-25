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
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.UserRepository;

class ExcelExporterTest {
    @TempDir
    Path tempDirectory;

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
        public Optional<User> findById(long id) {
            return users.stream().filter(user -> user.getId() == id).findFirst();
        }

        @Override
        public List<User> findAll() {
            return users;
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
}
