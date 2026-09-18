package ru.mirea.blogplatform.repository;

import java.util.List;
import java.util.Optional;
import ru.mirea.blogplatform.model.User;

public interface UserRepository {
    Optional<User> findById(long id);

    List<User> findAll();
}
