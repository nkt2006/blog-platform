package ru.mirea.blogplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import ru.mirea.blogplatform.exception.CommentException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.support.InMemoryCommentRepository;
import ru.mirea.blogplatform.support.InMemoryPostRepository;
import ru.mirea.blogplatform.support.InMemoryUserRepository;

class CommentServiceTest {
    @Test
    void crudSearchFiltersAndBothSortDirections() {
        Fixture fixture = new Fixture();
        Comment z = fixture.service.create(1, 1, "Заметка");
        Comment a = fixture.service.create(1, 1, "Авторский текст");
        a.setCreatedAt(z.getCreatedAt().plusMinutes(1));

        assertEquals(CommentStatus.PENDING, z.getStatus());
        assertEquals(2, fixture.service.list().size());
        assertEquals(a.getId(), fixture.service.searchByText("автор").get(0).getId());
        assertEquals(2, fixture.service.filterByPost(1).size());
        assertEquals(2, fixture.service.filterByStatus(CommentStatus.PENDING).size());
        assertEquals(a.getId(), fixture.service.sortByText(true).get(0).getId());
        assertEquals(z.getId(), fixture.service.sortByText(false).get(0).getId());
        assertEquals(z.getId(), fixture.service.sortByDate(false).get(0).getId());
        assertEquals(a.getId(), fixture.service.sortByDate(true).get(0).getId());

        fixture.service.update(z.getId(), "Другой текст");
        assertEquals("Другой текст", fixture.service.getById(z.getId()).getText());
        fixture.service.changeStatus(z.getId(), CommentStatus.APPROVED);
        assertEquals(1, fixture.service.filterByStatus(CommentStatus.APPROVED).size());
        fixture.service.delete(a.getId());
        assertEquals(1, fixture.service.list().size());
        assertThrows(CommentException.class, () -> fixture.service.getById(a.getId()));
    }

    @Test
    void validatesRelationsTextAndStatusTransitions() {
        Fixture fixture = new Fixture();
        assertThrows(CommentException.class, () -> fixture.service.create(99, 1, "Текст"));
        assertThrows(CommentException.class, () -> fixture.service.create(1, 99, "Текст"));
        assertThrows(CommentException.class, () -> fixture.service.create(1, 1, " "));
        Comment comment = fixture.service.create(1, 1, "Текст");
        assertThrows(CommentException.class,
                () -> fixture.service.changeStatus(comment.getId(), CommentStatus.PENDING));
        fixture.service.changeStatus(comment.getId(), CommentStatus.REJECTED);
        assertThrows(CommentException.class,
                () -> fixture.service.changeStatus(comment.getId(), CommentStatus.APPROVED));
        assertTrue(fixture.service.getById(comment.getId()).getStatus() == CommentStatus.REJECTED);
    }

    private static class Fixture {
        final CommentService service;

        Fixture() {
            InMemoryPostRepository posts = new InMemoryPostRepository();
            InMemoryUserRepository users = new InMemoryUserRepository();
            InMemoryCommentRepository comments = new InMemoryCommentRepository();
            users.add(new User(1L, "Автор", "author@example.com"));
            posts.create(new BlogPost(null, 1L, "Пост", "post", "Содержание",
                    PostStatus.DRAFT, null, null));
            service = new CommentService(comments, posts, users,
                    Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC));
        }
    }
}
