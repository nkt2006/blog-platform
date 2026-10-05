package ru.mirea.blogplatform.repository;

import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.model.User;

public interface UserRepository {
    User create(User user);

    Optional<User> findById(long id);

    List<User> findAll();

    boolean update(User user);

    boolean deleteById(long id);

    boolean existsByEmailAndIdNot(String email, long excludedId);
}
