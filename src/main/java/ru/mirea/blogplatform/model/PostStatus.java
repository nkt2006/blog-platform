package ru.mirea.blogplatform.model;

import java.util.Locale;

public enum PostStatus {
    DRAFT("Черновик"),
    REVIEW("На проверке"),
    PUBLISHED("Опубликована"),
    ARCHIVED("В архиве");

    private final String displayName;

    PostStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PostStatus fromUserInput(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Статус не указан");
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT).replace('ё', 'е');
        return switch (normalized) {
            case "draft", "черновик" -> DRAFT;
            case "review", "на проверке", "проверка" -> REVIEW;
            case "published", "опубликована", "опубликовано" -> PUBLISHED;
            case "archived", "в архиве", "архив" -> ARCHIVED;
            default -> throw new IllegalArgumentException("Неизвестный статус: " + value);
        };
    }
}
