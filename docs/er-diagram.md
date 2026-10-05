# ER-диаграмма блог-платформы

```mermaid
erDiagram
    USERS ||--o{ BLOG_POSTS : writes
    USERS ||--o{ COMMENTS : writes
    BLOG_POSTS ||--o{ COMMENTS : has

    USERS {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR role
    }

    BLOG_POSTS {
        BIGINT id PK
        BIGINT author_id FK
        VARCHAR title
        VARCHAR slug UK
        TEXT content
        VARCHAR status
        TIMESTAMP created_at
        TIMESTAMP published_at
    }

    COMMENTS {
        BIGINT id PK
        BIGINT post_id FK
        BIGINT author_id FK
        TEXT text
        VARCHAR status
        TIMESTAMP created_at
    }
```

Один автор может иметь несколько публикаций. Каждая публикация обязательно связана ровно с одним автором через `blog_posts.author_id → users.id`.
Комментарий принадлежит одной публикации и одному автору. У публикации и автора может быть несколько комментариев.
