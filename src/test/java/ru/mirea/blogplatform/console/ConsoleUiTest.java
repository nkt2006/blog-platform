package ru.mirea.blogplatform.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.service.BlogPostService;
import ru.mirea.blogplatform.service.CommentService;
import ru.mirea.blogplatform.service.UserService;
import ru.mirea.blogplatform.support.InMemoryCommentRepository;
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

        new ConsoleUi(service, new UserService(users),
                new CommentService(new InMemoryCommentRepository(), posts, users), input, output).run();

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
                "12", "1", "На проверке",
                "12", "1", "Опубликована",
                "4", "1", "", "Обновлённый заголовок", "", "",
                "13", "5", "1", "да", "0", "");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ConsoleUi(service, new UserService(users),
                new CommentService(new InMemoryCommentRepository(), posts, users),
                new Scanner(commands), output).run();

        String printed = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Создан черновик с ID 1."));
        assertTrue(printed.contains("Публикация обновлена."));
        assertTrue(printed.contains("Новый статус публикации 1: Опубликована"));
        assertTrue(printed.contains("Опубликованных: 1"));
        assertTrue(printed.contains("Публикация удалена."));
        assertEquals(0, posts.findAll().size());
    }

    @Test
    void menusManageAuthorsAndComments() {
        InMemoryPostRepository posts = new InMemoryPostRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryCommentRepository comments = new InMemoryCommentRepository();
        users.add(new User(1L, "Автор", "author@example.com"));
        posts.create(new BlogPost(null, 1L, "Пост", "post", "Текст",
                PostStatus.DRAFT, null, null));
        String commands = String.join("\n",
                "16", "1", "Пётр", "petr@example.com", "Редактор", "2", "0",
                "17", "1", "1", "1", "Отличный пост", "2", "6", "1", "Одобрен",
                "8", "Одобрен", "0", "0", "");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ConsoleUi(new BlogPostService(posts, users, comments), new UserService(users),
                new CommentService(comments, posts, users), new Scanner(commands), output).run();

        String printed = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Автор создан с ID 2."));
        assertTrue(printed.contains("Комментарий создан с ID 1."));
        assertTrue(printed.contains("Статус комментария: Одобрен"));
        assertTrue(printed.contains("До свидания!"));
        assertEquals(1, comments.findAll().size());
    }
}
