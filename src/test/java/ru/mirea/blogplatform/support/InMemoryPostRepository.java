package ru.mirea.blogplatform.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.repository.BlogPostRepository;

public class InMemoryPostRepository implements BlogPostRepository {
    private final Map<Long, BlogPost> posts = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public BlogPost create(BlogPost post) {
        post.setId(nextId++);
        posts.put(post.getId(), post);
        return post;
    }

    @Override
    public Optional<BlogPost> findById(long id) {
        return Optional.ofNullable(posts.get(id));
    }

    @Override
    public List<BlogPost> findAll() {
        return new ArrayList<>(posts.values());
    }

    @Override
    public boolean update(BlogPost post) {
        if (!posts.containsKey(post.getId())) {
            return false;
        }
        posts.put(post.getId(), post);
        return true;
    }

    @Override
    public boolean deleteById(long id) {
        return posts.remove(id) != null;
    }

    @Override
    public boolean existsBySlug(String slug) {
        return posts.values().stream().anyMatch(post -> post.getSlug().equals(slug));
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, long excludedId) {
        return posts.values().stream().anyMatch(post -> !post.getId().equals(excludedId)
                && post.getSlug().equals(slug));
    }
}
