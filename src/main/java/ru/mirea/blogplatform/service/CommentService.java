package ru.mirea.blogplatform.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import ru.mirea.blogplatform.exception.CommentException;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.CommentRepository;
import ru.mirea.blogplatform.repository.UserRepository;

public class CommentService {
    private final CommentRepository comments;
    private final BlogPostRepository posts;
    private final UserRepository users;
    private final Clock clock;

    public CommentService(CommentRepository comments, BlogPostRepository posts, UserRepository users) {
        this(comments, posts, users, Clock.systemDefaultZone());
    }

    public CommentService(CommentRepository comments, BlogPostRepository posts,
                          UserRepository users, Clock clock) {
        this.comments = Objects.requireNonNull(comments);
        this.posts = Objects.requireNonNull(posts);
        this.users = Objects.requireNonNull(users);
        this.clock = Objects.requireNonNull(clock);
    }

    public Comment create(long postId, long authorId, String text) {
        if (posts.findById(postId).isEmpty()) {
            throw new CommentException("Публикация с ID " + postId + " не найдена.");
        }
        if (users.findById(authorId).isEmpty()) {
            throw new CommentException("Автор с ID " + authorId + " не найден.");
        }
        return comments.create(new Comment(null, postId, authorId, required(text),
                CommentStatus.PENDING, LocalDateTime.now(clock)));
    }

    public Comment getById(long id) {
        return comments.findById(id).orElseThrow(() -> missing(id));
    }

    public List<Comment> list() {
        return comments.findAll();
    }

    public Comment update(long id, String text) {
        Comment comment = getById(id);
        comment.setText(required(text));
        if (!comments.update(comment)) {
            throw missing(id);
        }
        return comment;
    }

    public Comment changeStatus(long id, CommentStatus status) {
        Comment comment = getById(id);
        if (comment.getStatus() != CommentStatus.PENDING
                || status == null || status == CommentStatus.PENDING) {
            throw new CommentException("Комментарий можно перевести из проверки только в одобренные или отклонённые.");
        }
        comment.setStatus(status);
        if (!comments.update(comment)) {
            throw missing(id);
        }
        return comment;
    }

    public void delete(long id) {
        if (!comments.deleteById(id)) {
            throw missing(id);
        }
    }

    public List<Comment> searchByText(String query) {
        String needle = required(query).toLowerCase(Locale.ROOT);
        return comments.findAll().stream()
                .filter(comment -> comment.getText().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public List<Comment> filterByStatus(CommentStatus status) {
        if (status == null) {
            throw new CommentException("Укажите статус комментария.");
        }
        return comments.findAll().stream().filter(comment -> comment.getStatus() == status).toList();
    }

    public List<Comment> filterByPost(long postId) {
        if (posts.findById(postId).isEmpty()) {
            throw new CommentException("Публикация с ID " + postId + " не найдена.");
        }
        return comments.findAll().stream().filter(comment -> comment.getPostId() == postId).toList();
    }

    public List<Comment> sortByDate(boolean newestFirst) {
        Comparator<Comment> comparator = Comparator.comparing(Comment::getCreatedAt);
        return sorted(newestFirst ? comparator.reversed() : comparator);
    }

    public List<Comment> sortByText(boolean ascending) {
        Comparator<Comment> comparator = Comparator.comparing(Comment::getText, String.CASE_INSENSITIVE_ORDER);
        return sorted(ascending ? comparator : comparator.reversed());
    }

    private List<Comment> sorted(Comparator<Comment> comparator) {
        return comments.findAll().stream().sorted(comparator.thenComparing(Comment::getId)).toList();
    }

    private String required(String text) {
        if (text == null || text.isBlank()) {
            throw new CommentException("Текст комментария не может быть пустым.");
        }
        return text.strip();
    }

    private CommentException missing(long id) {
        return new CommentException("Комментарий с ID " + id + " не найден.");
    }
}
