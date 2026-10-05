package ru.mirea.blogplatform.model;

import java.time.LocalDateTime;

public class Comment {
    private Long id;
    private Long postId;
    private Long authorId;
    private String text;
    private CommentStatus status;
    private LocalDateTime createdAt;

    public Comment(Long id, Long postId, Long authorId, String text,
                   CommentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.text = text;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
