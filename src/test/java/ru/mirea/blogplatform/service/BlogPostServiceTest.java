package ru.mirea.blogplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.exception.EntityNotFoundException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.support.InMemoryPostRepository;
import ru.mirea.blogplatform.support.InMemoryUserRepository;

class BlogPostServiceTest {
    private InMemoryPostRepository posts;
    private BlogPostService service;

    @BeforeEach
    void setUp() {
        posts = new InMemoryPostRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        users.add(new User(1L, "Никита", "nikita@example.com"));
        users.add(new User(2L, "Максим", "max@example.com"));
        Clock clock = Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC);
        service = new BlogPostService(posts, users, clock);
    }

    @Test
    void creationAndStatusChangesEnforceBusinessRules() {
        assertThrows(BusinessRuleException.class,
                () -> service.createPost(1, " ", "empty-title", "Текст"));
        assertThrows(BusinessRuleException.class,
                () -> service.createPost(1, "Заголовок", "empty-content", " "));
        assertThrows(EntityNotFoundException.class,
                () -> service.createPost(99, "Заголовок", "missing-author", "Текст"));

        BlogPost post = service.createPost(1, " Java ", "JAVA-POST", "Текст");
        assertEquals("Java", post.getTitle());
        assertEquals("java-post", post.getSlug());
        assertEquals(PostStatus.DRAFT, post.getStatus());
        assertNull(post.getPublishedAt());
        assertThrows(BusinessRuleException.class,
                () -> service.createPost(2, "Ещё пост", "java-post", "Текст"));
        assertThrows(BusinessRuleException.class,
                () -> service.changeStatus(post.getId(), PostStatus.PUBLISHED));

        service.changeStatus(post.getId(), PostStatus.REVIEW);
        BlogPost published = service.changeStatus(post.getId(), PostStatus.PUBLISHED);
        LocalDateTime expectedDate = LocalDateTime.of(2026, 9, 18, 12, 0);
        assertEquals(expectedDate, published.getPublishedAt());
        assertEquals(expectedDate, service.changeStatus(post.getId(), PostStatus.ARCHIVED)
                .getPublishedAt());
        assertThrows(BusinessRuleException.class,
                () -> service.changeStatus(post.getId(), PostStatus.DRAFT));
    }

    @Test
    void editingSearchingFilteringSortingAndStatisticsWork() {
        BlogPost java = service.createPost(1, "Java Guide", "java-guide", "1234");
        BlogPost sql = service.createPost(2, "SQL Notes", "sql-notes", "123456");
        java.setCreatedAt(LocalDateTime.of(2026, 9, 17, 12, 0));
        service.changeStatus(sql.getId(), PostStatus.REVIEW);
        service.changeStatus(sql.getId(), PostStatus.PUBLISHED);

        assertEquals(1, service.searchByTitle("JAVA").size());
        assertEquals(1, service.searchByAuthor("ник").size());
        assertEquals(1, service.filterByStatus(PostStatus.PUBLISHED).size());
        assertEquals(1, service.filterByAuthor(2).size());
        assertEquals(sql.getId(), service.sortByCreationDate(true).get(0).getId());
        assertEquals(java.getId(), service.sortByTitle(true).get(0).getId());

        BlogStatistics stats = service.getStatistics();
        assertEquals(2, stats.getTotalPosts());
        assertEquals(1, stats.getPublishedPosts());
        assertEquals(1, stats.getDraftPosts());
        assertEquals(2, stats.getAuthorsWithPosts());
        assertEquals(5.0, stats.getAverageContentLength());

        service.updatePost(java.getId(), 1, "Java Basics", "java-basics", "Новый текст");
        assertEquals("Java Basics", service.getPostById(java.getId()).getTitle());
        assertThrows(BusinessRuleException.class,
                () -> service.updatePost(java.getId(), 1, "Java", "sql-notes", "Текст"));
        service.deletePost(java.getId());
        assertThrows(EntityNotFoundException.class, () -> service.getPostById(java.getId()));
        assertThrows(EntityNotFoundException.class, () -> service.deletePost(999));
    }
}
