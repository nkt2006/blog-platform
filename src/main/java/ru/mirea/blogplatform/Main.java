package ru.mirea.blogplatform;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.console.ConsoleUi;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.UserRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcBlogPostRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcUserRepository;
import ru.mirea.blogplatform.service.BlogPostService;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        DatabaseManager database = new DatabaseManager();
        UserRepository users = new JdbcUserRepository(database);
        BlogPostRepository posts = new JdbcBlogPostRepository(database);
        BlogPostService service = new BlogPostService(posts, users);

        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            new ConsoleUi(service, input, System.out).run();
        }
    }
}
