package ru.mirea.blogplatform.model;

import java.util.Locale;

public enum CommentStatus {
    PENDING("На проверке"),
    APPROVED("Одобрен"),
    REJECTED("Отклонён");

    private final String displayName;

    CommentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CommentStatus fromUserInput(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Статус комментария не указан.");
        }
        return switch (input.strip().toLowerCase(Locale.ROOT).replace('ё', 'е')) {
            case "pending", "на проверке" -> PENDING;
            case "approved", "одобрен" -> APPROVED;
            case "rejected", "отклонен" -> REJECTED;
            default -> throw new IllegalArgumentException("Неизвестный статус комментария: " + input);
        };
    }
}
