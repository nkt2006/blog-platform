package ru.mirea.blogplatform.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import ru.mirea.blogplatform.exception.UserException;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;
import ru.mirea.blogplatform.repository.UserRepository;

public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = Objects.requireNonNull(users);
    }

    public User create(String name, String email, UserRole role) {
        String validEmail = validEmail(email);
        if (users.existsByEmailAndIdNot(validEmail, -1)) {
            throw new UserException("Автор с таким email уже существует.");
        }
        return users.create(new User(null, required(name, "Имя"), validEmail, requiredRole(role)));
    }

    public User getById(long id) {
        return users.findById(id).orElseThrow(() -> missing(id));
    }

    public List<User> list() {
        return users.findAll();
    }

    public User update(long id, String name, String email, UserRole role) {
        getById(id);
        String validEmail = validEmail(email);
        if (users.existsByEmailAndIdNot(validEmail, id)) {
            throw new UserException("Автор с таким email уже существует.");
        }
        User user = new User(id, required(name, "Имя"), validEmail, requiredRole(role));
        if (!users.update(user)) {
            throw missing(id);
        }
        return user;
    }

    public void delete(long id) {
        if (!users.deleteById(id)) {
            throw missing(id);
        }
    }

    public List<User> searchByName(String query) {
        String needle = required(query, "Поисковый запрос").toLowerCase(Locale.ROOT);
        return users.findAll().stream()
                .filter(user -> user.getName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public List<User> filterByRole(UserRole role) {
        requiredRole(role);
        return users.findAll().stream().filter(user -> user.getRole() == role).toList();
    }

    public List<User> sortByName(boolean ascending) {
        return sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER), ascending);
    }

    public List<User> sortByEmail(boolean ascending) {
        return sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER), ascending);
    }

    private List<User> sorted(Comparator<User> comparator, boolean ascending) {
        Comparator<User> order = ascending ? comparator : comparator.reversed();
        return users.findAll().stream().sorted(order.thenComparing(User::getId)).toList();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new UserException(field + " не может быть пустым.");
        }
        return value.strip();
    }

    private String validEmail(String value) {
        String email = required(value, "Email").toLowerCase(Locale.ROOT);
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new UserException("Укажите корректный email.");
        }
        return email;
    }

    private UserRole requiredRole(UserRole role) {
        if (role == null) {
            throw new UserException("Роль автора не указана.");
        }
        return role;
    }

    private UserException missing(long id) {
        return new UserException("Автор с ID " + id + " не найден.");
    }
}
