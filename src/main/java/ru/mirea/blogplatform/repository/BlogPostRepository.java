package ru.mirea.blogplatform.repository;

import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.model.BlogPost;

public interface BlogPostRepository {
    /** Inserts a post and returns it with its database-generated ID. */
    BlogPost create(BlogPost post);

    Optional<BlogPost> findById(long id);

    /** Returns every post; callers choose the display order. */
    List<BlogPost> findAll();

    /** Returns false when no post with this ID exists. */
    boolean update(BlogPost post);

    /** Returns false when no post with this ID exists. */
    boolean deleteById(long id);

    /** Used to enforce unique slugs when creating a post. */
    boolean existsBySlug(String slug);

    /** Used to enforce unique slugs when updating a post. */
    boolean existsBySlugAndIdNot(String slug, long excludedId);
}
