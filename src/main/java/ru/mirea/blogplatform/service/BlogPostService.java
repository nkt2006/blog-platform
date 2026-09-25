package ru.mirea.blogplatform.service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.exception.EntityNotFoundException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.UserRepository;
import ru.mirea.blogplatform.util.ExcelExporter;

public class BlogPostService {
    private final BlogPostRepository posts;
    private final UserRepository users;
    private final Clock clock;
    private final ExcelExporter exporter;

    public BlogPostService(BlogPostRepository posts, UserRepository users) {
        this(posts, users, Clock.systemDefaultZone());
    }

    public BlogPostService(BlogPostRepository posts, UserRepository users, Clock clock) {
        this.posts = Objects.requireNonNull(posts);
        this.users = Objects.requireNonNull(users);
        this.clock = Objects.requireNonNull(clock);
        this.exporter = new ExcelExporter(users, posts);
    }

    public BlogPost createPost(long authorId, String title, String slug, String content) {
        requireAuthor(authorId);
        String validTitle = requireText(title, "Заголовок");
        String validContent = requireText(content, "Текст публикации");
        String validSlug = requireSlug(slug);
        if (posts.existsBySlug(validSlug)) {
            throw new BusinessRuleException("Публикация с таким адресом уже существует.");
        }

        BlogPost post = new BlogPost(null, authorId, validTitle, validSlug, validContent,
                PostStatus.DRAFT, LocalDateTime.now(clock), null);
        return posts.create(post);
    }

    public BlogPost updatePost(long id, long authorId, String title, String slug, String content) {
        BlogPost current = getPostById(id);
        requireAuthor(authorId);
        String validTitle = requireText(title, "Заголовок");
        String validContent = requireText(content, "Текст публикации");
        String validSlug = requireSlug(slug);
        if (posts.existsBySlugAndIdNot(validSlug, id)) {
            throw new BusinessRuleException("Публикация с таким адресом уже существует.");
        }

        BlogPost updated = new BlogPost(id, authorId, validTitle, validSlug, validContent,
                current.getStatus(), current.getCreatedAt(), current.getPublishedAt());
        if (!posts.update(updated)) {
            throw missingPost(id);
        }
        return updated;
    }

    public BlogPost changeStatus(long id, PostStatus nextStatus) {
        BlogPost current = getPostById(id);
        if (nextStatus == null || !isAllowedTransition(current.getStatus(), nextStatus)) {
            String nextStatusName = nextStatus == null
                    ? "не указан" : nextStatus.getDisplayName();
            throw new BusinessRuleException("Недопустимый переход статуса: "
                    + current.getStatus().getDisplayName() + " → " + nextStatusName + ".");
        }
        if (nextStatus == PostStatus.PUBLISHED) {
            requireText(current.getContent(), "Текст публикации");
        }

        LocalDateTime publishedAt = nextStatus == PostStatus.PUBLISHED
                ? LocalDateTime.now(clock) : current.getPublishedAt();
        BlogPost updated = new BlogPost(id, current.getAuthorId(), current.getTitle(),
                current.getSlug(), current.getContent(), nextStatus,
                current.getCreatedAt(), publishedAt);
        if (!posts.update(updated)) {
            throw missingPost(id);
        }
        return updated;
    }

    public BlogPost getPostById(long id) {
        return posts.findById(id).orElseThrow(() -> missingPost(id));
    }

    public List<BlogPost> listPosts() {
        return posts.findAll();
    }

    public List<User> listUsers() {
        return users.findAll();
    }

    public Path exportToExcel(Path file) {
        return exporter.export(file);
    }

    public void deletePost(long id) {
        if (!posts.deleteById(id)) {
            throw missingPost(id);
        }
    }

    public List<BlogPost> searchByTitle(String query) {
        String needle = requireText(query, "Поисковый запрос").toLowerCase(Locale.ROOT);
        return posts.findAll().stream()
                .filter(post -> post.getTitle().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public List<BlogPost> searchByAuthor(String query) {
        String needle = requireText(query, "Поисковый запрос").toLowerCase(Locale.ROOT);
        Set<Long> authorIds = users.findAll().stream()
                .filter(user -> user.getName().toLowerCase(Locale.ROOT).contains(needle))
                .map(User::getId)
                .collect(Collectors.toSet());
        return posts.findAll().stream()
                .filter(post -> authorIds.contains(post.getAuthorId()))
                .toList();
    }

    public List<BlogPost> filterByStatus(PostStatus status) {
        if (status == null) {
            throw new BusinessRuleException("Укажите статус публикации.");
        }
        return posts.findAll().stream()
                .filter(post -> post.getStatus() == status)
                .toList();
    }

    public List<BlogPost> filterByAuthor(long authorId) {
        requireAuthor(authorId);
        return posts.findAll().stream()
                .filter(post -> post.getAuthorId().equals(authorId))
                .toList();
    }

    public List<BlogPost> sortByCreationDate(boolean newestFirst) {
        Comparator<BlogPost> byDate = Comparator.comparing(BlogPost::getCreatedAt);
        if (newestFirst) {
            byDate = byDate.reversed();
        }
        return posts.findAll().stream().sorted(byDate.thenComparing(BlogPost::getId)).toList();
    }

    public List<BlogPost> sortByTitle(boolean ascending) {
        Comparator<BlogPost> byTitle = Comparator.comparing(BlogPost::getTitle,
                String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            byTitle = byTitle.reversed();
        }
        return posts.findAll().stream().sorted(byTitle.thenComparing(BlogPost::getId)).toList();
    }

    public BlogStatistics getStatistics() {
        List<BlogPost> all = posts.findAll();
        long published = all.stream().filter(post -> post.getStatus() == PostStatus.PUBLISHED).count();
        long drafts = all.stream().filter(post -> post.getStatus() == PostStatus.DRAFT).count();
        long authors = all.stream().map(BlogPost::getAuthorId).distinct().count();
        double averageLength = all.stream().mapToInt(post -> post.getContent().length())
                .average().orElse(0);
        return new BlogStatistics(all.size(), published, drafts, authors, averageLength);
    }

    private User requireAuthor(long authorId) {
        return users.findById(authorId)
                .orElseThrow(() -> new EntityNotFoundException("Автор с ID " + authorId + " не найден."));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(field + " не может быть пустым.");
        }
        return value.strip();
    }

    private String requireSlug(String value) {
        String slug = requireText(value, "Адрес публикации").toLowerCase(Locale.ROOT);
        if (!slug.matches("[\\p{L}\\p{N}]+(?:-[\\p{L}\\p{N}]+)*")) {
            throw new BusinessRuleException(
                    "Адрес публикации может содержать буквы, цифры и дефисы между словами."
            );
        }
        return slug;
    }

    private boolean isAllowedTransition(PostStatus current, PostStatus next) {
        return (current == PostStatus.DRAFT && next == PostStatus.REVIEW)
                || (current == PostStatus.REVIEW && next == PostStatus.PUBLISHED)
                || (current == PostStatus.PUBLISHED && next == PostStatus.ARCHIVED);
    }

    private EntityNotFoundException missingPost(long id) {
        return new EntityNotFoundException("Публикация с ID " + id + " не найдена.");
    }
}
