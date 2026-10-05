package ru.mirea.blogplatform.console;

import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import ru.mirea.blogplatform.exception.UserException;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;
import ru.mirea.blogplatform.service.UserService;

public class UserConsoleUi {
    private static final String MENU = """

            === Авторы ===
             1. Создать автора
             2. Показать всех
             3. Найти по ID
             4. Изменить автора
             5. Удалить автора
             6. Поиск по имени
             7. Фильтр по роли
             8. Сортировка по имени
             9. Сортировка по email
             0. Назад
            """;

    private final UserService service;
    private final Scanner input;
    private final PrintStream output;

    public UserConsoleUi(UserService service, Scanner input, PrintStream output) {
        this.service = Objects.requireNonNull(service);
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        while (true) {
            output.println(MENU);
            try {
                switch (Math.toIntExact(number("Выберите пункт: "))) {
                    case 0 -> { return; }
                    case 1 -> create();
                    case 2 -> print(service.list());
                    case 3 -> print(List.of(service.getById(number("ID автора: "))));
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> print(service.searchByName(read("Часть имени: ")));
                    case 7 -> print(service.filterByRole(role(read("Роль (Автор/Редактор): "))));
                    case 8 -> print(service.sortByName(order()));
                    case 9 -> print(service.sortByEmail(order()));
                    default -> output.println("Такого пункта нет.");
                }
            } catch (NoSuchElementException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                output.println("Ошибка: " + exception.getMessage());
            }
        }
    }

    private void create() {
        User user = service.create(read("Имя: "), read("Email: "),
                role(read("Роль (Автор/Редактор): ")));
        output.println("Автор создан с ID " + user.getId() + ".");
    }

    private void update() {
        long id = number("ID автора: ");
        User current = service.getById(id);
        String name = keep(read("Имя [" + current.getName() + "]: "), current.getName());
        String email = keep(read("Email [" + current.getEmail() + "]: "), current.getEmail());
        String enteredRole = read("Роль [" + current.getRole().getDisplayName() + "]: ");
        UserRole role = enteredRole.isBlank() ? current.getRole() : role(enteredRole);
        service.update(id, name, email, role);
        output.println("Автор обновлён.");
    }

    private void delete() {
        long id = number("ID автора: ");
        if (!read("Удалить автора? (да/нет): ").equalsIgnoreCase("да")) {
            output.println("Удаление отменено.");
            return;
        }
        service.delete(id);
        output.println("Автор удалён.");
    }

    private void print(List<User> users) {
        if (users.isEmpty()) {
            output.println("Авторы не найдены.");
            return;
        }
        for (User user : users) {
            output.printf("#%d | %s | %s | %s%n", user.getId(), user.getName(),
                    user.getEmail(), user.getRole().getDisplayName());
        }
        output.println("Всего: " + users.size());
    }

    private String read(String prompt) {
        output.print(prompt);
        return input.nextLine();
    }

    private long number(String prompt) {
        try {
            long value = Long.parseLong(read(prompt).strip());
            if (value < 0) {
                throw new UserException("Введите неотрицательное число.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new UserException("Введите целое число.");
        }
    }

    private boolean order() {
        String choice = read("1 — прямой порядок, 2 — обратный: ").strip();
        if (!choice.equals("1") && !choice.equals("2")) {
            throw new UserException("Выберите 1 или 2.");
        }
        return choice.equals("1");
    }

    private UserRole role(String value) {
        try {
            return UserRole.fromUserInput(value);
        } catch (IllegalArgumentException exception) {
            throw new UserException(exception.getMessage());
        }
    }

    private String keep(String entered, String previous) {
        return entered.isBlank() ? previous : entered;
    }
}
