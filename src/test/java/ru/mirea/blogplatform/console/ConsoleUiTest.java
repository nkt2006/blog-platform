package ru.mirea.blogplatform.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.service.BlogPostService;
import ru.mirea.blogplatform.support.InMemoryPostRepository;
import ru.mirea.blogplatform.support.InMemoryUserRepository;

class ConsoleUiTest {
    @Test
    void invalidInputDoesNotCloseMenu() {
        InMemoryPostRepository posts = new InMemoryPostRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        BlogPostService service = new BlogPostService(posts, users);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Scanner input = new Scanner("не число\n3\nabc\n99\n0\n");
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ConsoleUi(service, input, output).run();

        String printed = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Ошибка: введите целое число."));
        assertTrue(printed.contains("Такого пункта нет."));
        assertTrue(printed.contains("До свидания!"));
    }

    @Test
    void menuRunsPostLifecycle() {
        InMemoryPostRepository posts = new InMemoryPostRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        users.add(new User(1L, "Никита", "nikita@example.com"));
        BlogPostService service = new BlogPostService(posts, users);
        String commands = String.join("\n",
                "1", "1", "Первый пост", "first-post", "Текст",
                "12", "1", "REVIEW",
                "12", "1", "PUBLISHED",
                "4", "1", "", "Обновлённый заголовок", "", "",
                "13", "5", "1", "да", "0", "");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ConsoleUi(service, new Scanner(commands), output).run();

        String printed = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Создан черновик с ID 1."));
        assertTrue(printed.contains("Публикация обновлена."));
        assertTrue(printed.contains("Опубликованных: 1"));
        assertTrue(printed.contains("Публикация удалена."));
        assertEquals(0, posts.findAll().size());
    }
}
