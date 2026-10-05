package ru.mirea.blogplatform.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.repository.UserRepository;

public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    public void add(User user) {
        users.put(user.getId(), user);
        nextId = Math.max(nextId, user.getId() + 1);
    }

    @Override
    public User create(User user) {
        User created = new User(nextId++, user.getName(), user.getEmail(), user.getRole());
        add(created);
        return created;
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public boolean update(User user) {
        if (!users.containsKey(user.getId())) {
            return false;
        }
        users.put(user.getId(), user);
        return true;
    }

    @Override
    public boolean deleteById(long id) {
        return users.remove(id) != null;
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, long excludedId) {
        return users.values().stream().anyMatch(user -> user.getId() != excludedId
                && user.getEmail().equalsIgnoreCase(email));
    }
}
