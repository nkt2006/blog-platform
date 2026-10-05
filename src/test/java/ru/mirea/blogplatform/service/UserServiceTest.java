package ru.mirea.blogplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.mirea.blogplatform.exception.UserException;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;
import ru.mirea.blogplatform.support.InMemoryUserRepository;

class UserServiceTest {
    @Test
    void crudSearchFilterAndBothSortDirections() {
        UserService service = new UserService(new InMemoryUserRepository());
        User zoe = service.create("Зоя", "ZOE@example.com", UserRole.AUTHOR);
        User anna = service.create("Анна", "anna@example.com", UserRole.EDITOR);

        assertEquals("zoe@example.com", service.getById(zoe.getId()).getEmail());
        assertEquals(2, service.list().size());
        assertEquals(anna.getId(), service.searchByName("анн").get(0).getId());
        assertEquals(zoe.getId(), service.filterByRole(UserRole.AUTHOR).get(0).getId());
        assertEquals(anna.getId(), service.sortByName(true).get(0).getId());
        assertEquals(zoe.getId(), service.sortByName(false).get(0).getId());
        assertEquals(anna.getId(), service.sortByEmail(true).get(0).getId());
        assertEquals(zoe.getId(), service.sortByEmail(false).get(0).getId());

        service.update(zoe.getId(), "Зоя Новая", "new@example.com", UserRole.EDITOR);
        assertEquals(UserRole.EDITOR, service.getById(zoe.getId()).getRole());
        service.delete(anna.getId());
        assertEquals(1, service.list().size());
        assertThrows(UserException.class, () -> service.getById(anna.getId()));
    }

    @Test
    void rejectsInvalidAndDuplicateEmails() {
        UserService service = new UserService(new InMemoryUserRepository());
        User created = service.create("Автор", "author@example.com", UserRole.AUTHOR);
        assertThrows(UserException.class, () -> service.create("Другой", "AUTHOR@example.com", UserRole.EDITOR));
        assertThrows(UserException.class, () -> service.create("", "new@example.com", UserRole.AUTHOR));
        assertThrows(UserException.class, () -> service.create("Другой", "wrong", UserRole.AUTHOR));
        assertTrue(service.getById(created.getId()).getName().equals("Автор"));
    }
}
