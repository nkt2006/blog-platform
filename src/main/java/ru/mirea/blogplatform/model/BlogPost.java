package ru.mirea.blogplatform.model;

import java.time.LocalDateTime;

public class BlogPost {
    private Long id;
    private Long authorId;
    private String title;
    private String slug;
    private String content;
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public BlogPost(Long id, Long authorId, String title, String slug, String content,
                    PostStatus status, LocalDateTime createdAt, LocalDateTime publishedAt) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
