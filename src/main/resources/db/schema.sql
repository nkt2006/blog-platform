BEGIN;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    email VARCHAR(255) NOT NULL UNIQUE CHECK (btrim(email) <> '')
);

CREATE TABLE IF NOT EXISTS blog_posts (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL CHECK (btrim(title) <> ''),
    slug VARCHAR(200) NOT NULL UNIQUE CHECK (btrim(slug) <> ''),
    content TEXT NOT NULL CHECK (btrim(content) <> ''),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    CONSTRAINT fk_blog_posts_author
        FOREIGN KEY (author_id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_blog_posts_status
        CHECK (status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_blog_posts_publication_date
        CHECK (
            (status IN ('DRAFT', 'REVIEW') AND published_at IS NULL)
            OR
            (status IN ('PUBLISHED', 'ARCHIVED') AND published_at IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_blog_posts_author_id ON blog_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_blog_posts_status ON blog_posts(status);
CREATE INDEX IF NOT EXISTS idx_blog_posts_created_at ON blog_posts(created_at);

COMMIT;
