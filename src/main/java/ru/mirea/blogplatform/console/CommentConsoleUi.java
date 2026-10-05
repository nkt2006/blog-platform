package ru.mirea.blogplatform.console;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import ru.mirea.blogplatform.exception.CommentException;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.service.CommentService;

public class CommentConsoleUi {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String MENU = """

            === Комментарии ===
             1. Создать комментарий
             2. Показать все
             3. Найти по ID
             4. Изменить текст
             5. Удалить комментарий
             6. Изменить статус
             7. Поиск по тексту
             8. Фильтр по статусу
             9. Фильтр по публикации
            10. Сортировка по дате
            11. Сортировка по тексту
             0. Назад
            """;

    private final CommentService service;
    private final Scanner input;
    private final PrintStream output;

    public CommentConsoleUi(CommentService service, Scanner input, PrintStream output) {
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
                    case 3 -> print(List.of(service.getById(number("ID комментария: "))));
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> changeStatus();
                    case 7 -> print(service.searchByText(read("Часть текста: ")));
                    case 8 -> print(service.filterByStatus(status(read("Статус: "))));
                    case 9 -> print(service.filterByPost(number("ID публикации: ")));
                    case 10 -> print(service.sortByDate(!order()));
                    case 11 -> print(service.sortByText(order()));
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
        Comment comment = service.create(number("ID публикации: "),
                number("ID автора: "), read("Текст комментария: "));
        output.println("Комментарий создан с ID " + comment.getId() + ".");
    }

    private void update() {
        long id = number("ID комментария: ");
        Comment current = service.getById(id);
        String text = read("Новый текст [Enter = оставить прежний]: ");
        service.update(id, text.isBlank() ? current.getText() : text);
        output.println("Комментарий обновлён.");
    }

    private void delete() {
        long id = number("ID комментария: ");
        if (!read("Удалить комментарий? (да/нет): ").equalsIgnoreCase("да")) {
            output.println("Удаление отменено.");
            return;
        }
        service.delete(id);
        output.println("Комментарий удалён.");
    }

    private void changeStatus() {
        long id = number("ID комментария: ");
        Comment comment = service.changeStatus(id, status(read("Статус (Одобрен/Отклонён): ")));
        output.println("Статус комментария: " + comment.getStatus().getDisplayName());
    }

    private void print(List<Comment> comments) {
        if (comments.isEmpty()) {
            output.println("Комментарии не найдены.");
            return;
        }
        for (Comment comment : comments) {
            output.printf("#%d | публикация #%d | автор #%d | %s | %s | %s%n",
                    comment.getId(), comment.getPostId(), comment.getAuthorId(),
                    comment.getStatus().getDisplayName(), comment.getCreatedAt().format(DATE_FORMAT),
                    comment.getText());
        }
        output.println("Всего: " + comments.size());
    }

    private String read(String prompt) {
        output.print(prompt);
        return input.nextLine();
    }

    private long number(String prompt) {
        try {
            long value = Long.parseLong(read(prompt).strip());
            if (value < 0) {
                throw new CommentException("Введите неотрицательное число.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new CommentException("Введите целое число.");
        }
    }

    private boolean order() {
        String choice = read("1 — прямой порядок, 2 — обратный: ").strip();
        if (!choice.equals("1") && !choice.equals("2")) {
            throw new CommentException("Выберите 1 или 2.");
        }
        return choice.equals("1");
    }

    private CommentStatus status(String value) {
        try {
            return CommentStatus.fromUserInput(value);
        } catch (IllegalArgumentException exception) {
            throw new CommentException(exception.getMessage());
        }
    }
}
