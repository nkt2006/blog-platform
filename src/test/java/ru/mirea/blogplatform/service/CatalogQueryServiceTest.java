package ru.mirea.blogplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.model.*;

class CatalogQueryServiceTest {
    private final User user = new User(1L, "Никита", "nikita@example.com", UserRole.EDITOR);
    private final BlogPost post = new BlogPost(1L, 1L, "JavaFX", "javafx", "Работа с таблицами",
            PostStatus.DRAFT, LocalDateTime.of(2026, 10, 10, 12, 0), null);
    private final Comment comment = new Comment(1L, 1L, 1L, "Полезный пример", CommentStatus.PENDING, post.getCreatedAt());
    private final WorkspaceSnapshot data = new WorkspaceSnapshot(List.of(user), List.of(post), List.of(comment));
    private final CatalogQueryService service = new CatalogQueryService();

    @Test void searchesSeveralFieldsAndCombinesFilters() {
        assertTrue(service.matchesPost(post, data, "ТАБЛИЦ", PostStatus.DRAFT, user,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10)));
        assertTrue(service.matchesPost(post, data, "ник", null, null, null, null));
        assertFalse(service.matchesPost(post, data, "таблиц", PostStatus.PUBLISHED, user, null, null));
        assertFalse(service.matchesPost(post, data, "", null, null, LocalDate.of(2026, 10, 11), null));
        assertTrue(service.matchesUser(user, "EXAMPLE", UserRole.EDITOR));
        assertFalse(service.matchesUser(user, "EXAMPLE", UserRole.AUTHOR));
        assertTrue(service.matchesComment(comment, data, "javafx", CommentStatus.PENDING, post, user));
        assertFalse(service.matchesComment(comment, data, "javafx", CommentStatus.APPROVED, post, user));
    }

    @Test void rejectsReversedDateRange() {
        assertThrows(BusinessRuleException.class, () -> service.validateDates(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 10)));
    }
}
