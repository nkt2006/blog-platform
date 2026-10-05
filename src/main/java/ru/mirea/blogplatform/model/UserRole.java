package ru.mirea.blogplatform.model;

import java.util.Locale;

public enum UserRole {
    AUTHOR("Автор"),
    EDITOR("Редактор");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromUserInput(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Роль не указана.");
        }
        return switch (input.strip().toLowerCase(Locale.ROOT)) {
            case "author", "автор" -> AUTHOR;
            case "editor", "редактор" -> EDITOR;
            default -> throw new IllegalArgumentException("Неизвестная роль: " + input);
        };
    }
}
