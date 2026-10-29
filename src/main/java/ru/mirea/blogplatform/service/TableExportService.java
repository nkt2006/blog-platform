package ru.mirea.blogplatform.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.blogplatform.exception.DataExportException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.User;

public class TableExportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public ExportTable posts(List<BlogPost> posts, WorkspaceSnapshot data) {
        return new ExportTable("Публикации", List.of("Заголовок", "Автор", "Адрес", "Статус", "Создано", "Опубликовано", "Текст"),
                posts.stream().map(post -> List.of(post.getTitle(), data.authorName(post.getAuthorId()), post.getSlug(),
                        post.getStatus().getDisplayName(), date(post.getCreatedAt()), date(post.getPublishedAt()), post.getContent())).toList());
    }

    public ExportTable users(List<User> users) {
        return new ExportTable("Авторы", List.of("Имя", "Email", "Роль"),
                users.stream().map(user -> List.of(user.getName(), user.getEmail(), user.getRole().getDisplayName())).toList());
    }

    public ExportTable comments(List<Comment> comments, WorkspaceSnapshot data) {
        return new ExportTable("Комментарии", List.of("Публикация", "Автор", "Текст", "Статус", "Создано"),
                comments.stream().map(comment -> List.of(data.postTitle(comment.getPostId()), data.authorName(comment.getAuthorId()),
                        comment.getText(), comment.getStatus().getDisplayName(), date(comment.getCreatedAt()))).toList());
    }

    public Path export(ExportTable data, Path destination) {
        Path path = destination.toAbsolutePath().normalize();
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".csv")) {
            throw new IllegalArgumentException("Укажите расширение .xlsx или .csv.");
        }
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            if (filename.endsWith(".csv")) writeCsv(data, path);
            else writeExcel(data, path);
            return path;
        } catch (IOException error) {
            throw new DataExportException("Не удалось сохранить файл. Проверьте путь, права доступа и закройте файл в Excel.", error);
        }
    }

    private void writeExcel(ExportTable data, Path path) throws IOException {
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet(data.title());
            CellStyle style = book.createCellStyle();
            Font font = book.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row header = sheet.createRow(0);
            for (int col = 0; col < data.headers().size(); col++) {
                header.createCell(col).setCellValue(data.headers().get(col));
                header.getCell(col).setCellStyle(style);
            }
            for (int row = 0; row < data.rows().size(); row++) {
                Row target = sheet.createRow(row + 1);
                for (int col = 0; col < data.headers().size(); col++) {
                    String value = data.rows().get(row).get(col);
                    if (value.length() > 32767) {
                        throw new IllegalArgumentException("Текст длиннее лимита ячейки Excel (32767 символов). Используйте экспорт CSV.");
                    }
                    target.createCell(col).setCellValue(value);
                }
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, data.rows().size(), 0, data.headers().size() - 1));
            for (int col = 0; col < data.headers().size(); col++) {
                sheet.autoSizeColumn(col);
                sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + 512, 60 * 256));
            }
            try (OutputStream output = Files.newOutputStream(path)) { book.write(output); }
        }
    }

    private void writeCsv(ExportTable data, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            csvRow(writer, data.headers());
            for (List<String> row : data.rows()) csvRow(writer, row);
        }
    }

    private void csvRow(BufferedWriter writer, List<String> row) throws IOException {
        for (int index = 0; index < row.size(); index++) {
            if (index > 0) writer.write(';');
            String cell = row.get(index);
            if (!cell.isEmpty() && "=+-@".indexOf(cell.charAt(0)) >= 0) cell = "'" + cell;
            writer.write('"');
            writer.write(cell.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.newLine();
    }

    private String date(LocalDateTime value) { return value == null ? "" : DATE.format(value); }
}
