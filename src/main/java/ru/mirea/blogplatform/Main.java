package ru.mirea.blogplatform;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.console.ConsoleUi;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.CommentRepository;
import ru.mirea.blogplatform.repository.UserRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcBlogPostRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcCommentRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcUserRepository;
import ru.mirea.blogplatform.service.BlogPostService;
import ru.mirea.blogplatform.service.CommentService;
import ru.mirea.blogplatform.service.UserService;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        DatabaseManager database = new DatabaseManager();
        UserRepository users = new JdbcUserRepository(database);
        BlogPostRepository posts = new JdbcBlogPostRepository(database);
        CommentRepository comments = new JdbcCommentRepository(database);
        BlogPostService service = new BlogPostService(posts, users, comments);
        UserService userService = new UserService(users);
        CommentService commentService = new CommentService(comments, posts, users);

        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            new ConsoleUi(service, userService, commentService, input, System.out).run();
        }
    }
}
