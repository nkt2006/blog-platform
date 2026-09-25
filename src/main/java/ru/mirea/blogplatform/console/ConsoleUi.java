package ru.mirea.blogplatform.console;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.mirea.blogplatform.exception.BlogPlatformException;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.service.BlogPostService;
import ru.mirea.blogplatform.service.BlogStatistics;

public class ConsoleUi {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final BlogPostService service;
    private final Scanner input;
    private final PrintStream output;

    public ConsoleUi(BlogPostService service, Scanner input, PrintStream output) {
        this.service = Objects.requireNonNull(service);
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        while (true) {
            printMenu();
            try {
                int command = Integer.parseInt(readLine("Выберите пункт: "));
                if (command == 0) {
                    output.println("До свидания!");
                    return;
                }
                execute(command);
            } catch (NoSuchElementException ex) {
                output.println("\nВвод закрыт. Программа завершена.");
                return;
            } catch (NumberFormatException ex) {
                output.println("Ошибка: введите целое число.");
            } catch (BlogPlatformException | IllegalArgumentException ex) {
                output.println("Ошибка: " + ex.getMessage());
            } catch (RuntimeException ex) {
                output.println("Ошибка приложения или базы данных: " + ex.getMessage());
            }
        }
    }

    private void execute(int command) {
        switch (command) {
            case 1 -> createPost();
            case 2 -> printPosts(service.listPosts());
            case 3 -> printDetails(service.getPostById(readPositiveLong("ID публикации: ")));
            case 4 -> updatePost();
            case 5 -> deletePost();
            case 6 -> printPosts(service.searchByTitle(readLine("Часть заголовка: ")));
            case 7 -> printPosts(service.searchByAuthor(readLine("Имя автора или его часть: ")));
            case 8 -> printPosts(service.filterByStatus(readStatus("Статус: ")));
            case 9 -> printPosts(service.filterByAuthor(readPositiveLong("ID автора: ")));
            case 10 -> printPosts(service.sortByCreationDate(readFirstOption(
                    "1 — сначала новые, 2 — сначала старые: ")));
            case 11 -> printPosts(service.sortByTitle(readFirstOption(
                    "1 — от А до Я, 2 — от Я до А: ")));
            case 12 -> changeStatus();
            case 13 -> printStatistics();
            case 14 -> printUsers();
            case 15 -> exportExcel();
            default -> output.println("Такого пункта нет.");
        }
    }

    private void exportExcel() {
        String file = readLine("Путь к Excel-файлу [exports/blog-platform.xlsx]: ").strip();
        Path destination = file.isEmpty()
                ? Path.of("exports", "blog-platform.xlsx") : Path.of(file);
        Path saved = service.exportToExcel(destination);
        output.println("Excel-файл сохранён: " + saved);
    }

    private void createPost() {
        printUsers();
        long authorId = readPositiveLong("ID автора: ");
        String title = readLine("Заголовок: ");
        String slug = readLine("Адрес публикации (буквы, цифры, дефисы): ");
        String content = readLine("Текст публикации: ");
        BlogPost post = service.createPost(authorId, title, slug, content);
        output.println("Создан черновик с ID " + post.getId() + ".");
    }

    private void updatePost() {
        long id = readPositiveLong("ID публикации: ");
        BlogPost current = service.getPostById(id);
        printDetails(current);
        output.println("Нажмите Enter, чтобы оставить текущее значение.");
        String authorInput = readLine("ID автора [" + current.getAuthorId() + "]: ");
        long authorId = authorInput.isBlank() ? current.getAuthorId() : parsePositiveLong(authorInput);
        String title = readOrKeep("Заголовок", current.getTitle());
        String slug = readOrKeep("Адрес публикации", current.getSlug());
        String content = readOrKeep("Текст", current.getContent());
        service.updatePost(id, authorId, title, slug, content);
        output.println("Публикация обновлена.");
    }

    private void deletePost() {
        long id = readPositiveLong("ID публикации: ");
        String answer = readLine("Удалить публикацию " + id + "? (да/нет): ").strip();
        if (!answer.equalsIgnoreCase("да")) {
            output.println("Удаление отменено.");
            return;
        }
        service.deletePost(id);
        output.println("Публикация удалена.");
    }

    private void changeStatus() {
        long id = readPositiveLong("ID публикации: ");
        output.println("Текущий статус: " + service.getPostById(id).getStatus());
        PostStatus next = readStatus("Новый статус: ");
        BlogPost post = service.changeStatus(id, next);
        output.println("Новый статус публикации " + post.getId() + ": " + post.getStatus());
    }

    private void printPosts(List<BlogPost> posts) {
        if (posts.isEmpty()) {
            output.println("Публикации не найдены.");
            return;
        }
        Map<Long, User> usersById = service.listUsers().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (BlogPost post : posts) {
            User author = usersById.get(post.getAuthorId());
            String authorName = author == null ? "ID " + post.getAuthorId() : author.getName();
            output.printf("#%d | %s | %s | %s | создано %s%n",
                    post.getId(), post.getTitle(), authorName, post.getStatus(),
                    formatDate(post.getCreatedAt()));
        }
        output.println("Всего: " + posts.size());
    }

    private void printDetails(BlogPost post) {
        output.println("ID: " + post.getId());
        output.println("Заголовок: " + post.getTitle());
        output.println("Адрес публикации: " + post.getSlug());
        output.println("ID автора: " + post.getAuthorId());
        output.println("Статус: " + post.getStatus());
        output.println("Создано: " + formatDate(post.getCreatedAt()));
        output.println("Опубликовано: " + formatDate(post.getPublishedAt()));
        output.println("Текст: " + post.getContent());
    }

    private void printUsers() {
        List<User> users = service.listUsers();
        if (users.isEmpty()) {
            output.println("Авторы не найдены.");
            return;
        }
        output.println("Авторы:");
        for (User user : users) {
            output.printf("#%d | %s | %s%n", user.getId(), user.getName(), user.getEmail());
        }
    }

    private void printStatistics() {
        BlogStatistics stats = service.getStatistics();
        output.println("Всего публикаций: " + stats.getTotalPosts());
        output.println("Опубликованных: " + stats.getPublishedPosts());
        output.println("Черновиков: " + stats.getDraftPosts());
        output.println("Авторов с публикациями: " + stats.getAuthorsWithPosts());
        output.printf(Locale.ROOT, "Средняя длина текста: %.1f символов%n",
                stats.getAverageContentLength());
    }

    private String readLine(String prompt) {
        output.print(prompt);
        return input.nextLine();
    }

    private String readOrKeep(String field, String current) {
        String value = readLine(field + " (Enter = без изменений): ");
        return value.isBlank() ? current : value;
    }

    private long readPositiveLong(String prompt) {
        return parsePositiveLong(readLine(prompt));
    }

    private long parsePositiveLong(String value) {
        long number = Long.parseLong(value.strip());
        if (number <= 0) {
            throw new BusinessRuleException("ID должен быть положительным числом.");
        }
        return number;
    }

    private PostStatus readStatus(String prompt) {
        output.println("Варианты: DRAFT, REVIEW, PUBLISHED, ARCHIVED");
        String value = readLine(prompt).strip().toUpperCase(Locale.ROOT);
        try {
            return PostStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Неизвестный статус: " + value);
        }
    }

    private boolean readFirstOption(String prompt) {
        String value = readLine(prompt).strip();
        if (!value.equals("1") && !value.equals("2")) {
            throw new BusinessRuleException("Выберите 1 или 2.");
        }
        return value.equals("1");
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }

    private void printMenu() {
        output.println("\n=== Блог-платформа ===");
        output.println(" 1. Создать публикацию");
        output.println(" 2. Показать все публикации");
        output.println(" 3. Найти публикацию по ID");
        output.println(" 4. Изменить публикацию");
        output.println(" 5. Удалить публикацию");
        output.println(" 6. Поиск по заголовку");
        output.println(" 7. Поиск по автору");
        output.println(" 8. Фильтр по статусу");
        output.println(" 9. Фильтр по автору");
        output.println("10. Сортировка по дате создания");
        output.println("11. Сортировка по заголовку");
        output.println("12. Изменить статус");
        output.println("13. Статистика");
        output.println("14. Показать авторов");
        output.println("15. Экспорт в Excel (.xlsx)");
        output.println(" 0. Выход");
    }
}
