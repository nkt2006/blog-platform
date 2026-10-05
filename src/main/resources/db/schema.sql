BEGIN;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL CHECK (btrim(name) <> ''),
    email VARCHAR(255) NOT NULL UNIQUE CHECK (btrim(email) <> ''),
    role VARCHAR(20) NOT NULL DEFAULT 'AUTHOR'
        CONSTRAINT chk_users_role CHECK (role IN ('AUTHOR', 'EDITOR'))
);

-- Upgrade databases created with the earlier two-entity version.
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'AUTHOR';

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_role') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('AUTHOR', 'EDITOR'));
    END IF;
END $$;

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

CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    text TEXT NOT NULL CHECK (btrim(text) <> ''),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comments_seed UNIQUE (post_id, author_id, text)
);

CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_status ON comments(status);

COMMIT;
