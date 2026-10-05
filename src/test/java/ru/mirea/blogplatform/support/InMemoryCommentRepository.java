package ru.mirea.blogplatform.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.repository.CommentRepository;

public class InMemoryCommentRepository implements CommentRepository {
    private final Map<Long, Comment> comments = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Comment create(Comment comment) {
        Comment created = new Comment(nextId++, comment.getPostId(), comment.getAuthorId(),
                comment.getText(), comment.getStatus(), comment.getCreatedAt());
        comments.put(created.getId(), created);
        return created;
    }

    @Override
    public Optional<Comment> findById(long id) {
        return Optional.ofNullable(comments.get(id));
    }

    @Override
    public List<Comment> findAll() {
        return new ArrayList<>(comments.values());
    }

    @Override
    public boolean update(Comment comment) {
        if (!comments.containsKey(comment.getId())) {
            return false;
        }
        comments.put(comment.getId(), comment);
        return true;
    }

    @Override
    public boolean deleteById(long id) {
        return comments.remove(id) != null;
    }
}
