package ru.mirea.blogplatform.service;

import java.util.List;

/** Независимая от JavaFX таблица: экспортирует ровно те строки, которые видит пользователь. */
public record ExportTable(String title, List<String> headers, List<List<String>> rows) {
    public ExportTable {
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
        int columnCount = headers.size();
        if (rows.stream().anyMatch(row -> row.size() != columnCount)) {
            throw new IllegalArgumentException("Число ячеек не соответствует числу столбцов.");
        }
    }
}
