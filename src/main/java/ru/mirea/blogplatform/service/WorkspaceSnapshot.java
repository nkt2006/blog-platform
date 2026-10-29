package ru.mirea.blogplatform.service;

import java.util.List;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.User;

/** Снимок данных для согласованного обновления таблиц и списков выбора. */
public record WorkspaceSnapshot(List<User> users, List<BlogPost> posts, List<Comment> comments) {
    public WorkspaceSnapshot {
        users = List.copyOf(users);
        posts = List.copyOf(posts);
        comments = List.copyOf(comments);
    }

    public static WorkspaceSnapshot empty() {
        return new WorkspaceSnapshot(List.of(), List.of(), List.of());
    }

    public String authorName(Long id) {
        return users.stream().filter(user -> user.getId().equals(id))
                .map(User::getName).findFirst().orElse("Автор недоступен");
    }

    public String postTitle(Long id) {
        return posts.stream().filter(post -> post.getId().equals(id))
                .map(BlogPost::getTitle).findFirst().orElse("Публикация недоступна");
    }
}
