package ru.mirea.blogplatform.service;

import java.time.LocalDate;
import java.util.Locale;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;

/** Поиск по нескольким полям и совместное применение фильтров. SQL здесь отсутствует. */
public class CatalogQueryService {
    public boolean matchesPost(BlogPost post, WorkspaceSnapshot data, String query,
                               PostStatus status, User author, LocalDate from, LocalDate to) {
        validateDates(from, to);
        LocalDate date = post.getCreatedAt().toLocalDate();
        return contains(query, post.getTitle(), post.getContent(), post.getSlug(), data.authorName(post.getAuthorId()))
                && (status == null || post.getStatus() == status)
                && (author == null || post.getAuthorId().equals(author.getId()))
                && (from == null || !date.isBefore(from))
                && (to == null || !date.isAfter(to));
    }

    public boolean matchesUser(User user, String query, UserRole role) {
        return contains(query, user.getName(), user.getEmail()) && (role == null || user.getRole() == role);
    }

    public boolean matchesComment(Comment comment, WorkspaceSnapshot data, String query,
                                  CommentStatus status, BlogPost post, User author) {
        return contains(query, comment.getText(), data.authorName(comment.getAuthorId()), data.postTitle(comment.getPostId()))
                && (status == null || comment.getStatus() == status)
                && (post == null || comment.getPostId().equals(post.getId()))
                && (author == null || comment.getAuthorId().equals(author.getId()));
    }

    public void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("Начальная дата не может быть позже конечной.");
        }
    }

    private boolean contains(String query, String... values) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
