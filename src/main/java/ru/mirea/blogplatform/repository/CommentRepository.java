package ru.mirea.blogplatform.repository;

import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.model.Comment;

public interface CommentRepository {
    Comment create(Comment comment);
    Optional<Comment> findById(long id);
    List<Comment> findAll();
    boolean update(Comment comment);
    boolean deleteById(long id);
}
