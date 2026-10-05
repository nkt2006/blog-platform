package ru.mirea.blogplatform.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.blogplatform.exception.DataExportException;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.CommentRepository;
import ru.mirea.blogplatform.repository.UserRepository;

/** Exports current repository data to an Excel workbook. */
public class ExcelExporter {
    private static final String[] USER_HEADERS = {"ID", "Имя", "Email", "Роль"};
    private static final String[] POST_HEADERS = {
            "ID", "ID автора", "Заголовок", "Адрес публикации", "Статус",
            "Создано", "Опубликовано", "Содержимое"
    };
    private static final String[] COMMENT_HEADERS = {
            "ID", "ID публикации", "ID автора", "Текст", "Статус", "Создано"
    };

    private final UserRepository userRepository;
    private final BlogPostRepository blogPostRepository;
    private final CommentRepository commentRepository;

    public ExcelExporter(
            UserRepository userRepository,
            BlogPostRepository blogPostRepository
    ) {
        this(userRepository, blogPostRepository, null);
    }

    public ExcelExporter(
            UserRepository userRepository,
            BlogPostRepository blogPostRepository,
            CommentRepository commentRepository
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "Репозиторий авторов не указан"
        );
        this.blogPostRepository = Objects.requireNonNull(
                blogPostRepository, "Репозиторий публикаций не указан"
        );
        this.commentRepository = commentRepository;
    }

    public Path export(Path outputFile) {
        Objects.requireNonNull(outputFile, "Файл экспорта не указан");
        Path absoluteOutput = outputFile.toAbsolutePath().normalize();

        try {
            Path parent = absoluteOutput.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dateTimeStyle = createDateTimeStyle(workbook);
                writeUsers(workbook, userRepository.findAll(), headerStyle);
                writePosts(workbook, blogPostRepository.findAll(), headerStyle, dateTimeStyle);
                if (commentRepository != null) {
                    writeComments(workbook, commentRepository.findAll(), headerStyle, dateTimeStyle);
                }

                try (OutputStream outputStream = Files.newOutputStream(absoluteOutput)) {
                    workbook.write(outputStream);
                }
            }
            return absoluteOutput;
        } catch (IOException exception) {
            throw new DataExportException(
                    "Не удалось экспортировать данные в " + absoluteOutput,
                    exception
            );
        }
    }

    private void writeUsers(Workbook workbook, List<User> users, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Авторы");
        createHeader(sheet, USER_HEADERS, headerStyle);

        int rowIndex = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);
            setLong(row, 0, user.getId());
            row.createCell(1).setCellValue(user.getName());
            row.createCell(2).setCellValue(user.getEmail());
            row.createCell(3).setCellValue(user.getRole().getDisplayName());
        }

        finishSheet(sheet, USER_HEADERS.length, rowIndex);
    }

    private void writeComments(Workbook workbook, List<Comment> comments,
                               CellStyle headerStyle, CellStyle dateTimeStyle) {
        Sheet sheet = workbook.createSheet("Комментарии");
        createHeader(sheet, COMMENT_HEADERS, headerStyle);

        int rowIndex = 1;
        for (Comment comment : comments) {
            Row row = sheet.createRow(rowIndex++);
            setLong(row, 0, comment.getId());
            setLong(row, 1, comment.getPostId());
            setLong(row, 2, comment.getAuthorId());
            row.createCell(3).setCellValue(comment.getText());
            row.createCell(4).setCellValue(comment.getStatus().getDisplayName());
            setDateTime(row, 5, comment.getCreatedAt(), dateTimeStyle);
        }

        finishSheet(sheet, COMMENT_HEADERS.length, rowIndex);
        sheet.setColumnWidth(3, 60 * 256);
    }

    private void writePosts(
            Workbook workbook,
            List<BlogPost> posts,
            CellStyle headerStyle,
            CellStyle dateTimeStyle
    ) {
        Sheet sheet = workbook.createSheet("Публикации");
        createHeader(sheet, POST_HEADERS, headerStyle);

        int rowIndex = 1;
        for (BlogPost post : posts) {
            Row row = sheet.createRow(rowIndex++);
            setLong(row, 0, post.getId());
            setLong(row, 1, post.getAuthorId());
            row.createCell(2).setCellValue(post.getTitle());
            row.createCell(3).setCellValue(post.getSlug());
            row.createCell(4).setCellValue(post.getStatus().getDisplayName());
            setDateTime(row, 5, post.getCreatedAt(), dateTimeStyle);
            setDateTime(row, 6, post.getPublishedAt(), dateTimeStyle);
            row.createCell(7).setCellValue(post.getContent());
        }

        finishSheet(sheet, POST_HEADERS.length, rowIndex);
        sheet.setColumnWidth(7, 60 * 256);
    }

    private void createHeader(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        for (int column = 0; column < headers.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    private void finishSheet(Sheet sheet, int columnCount, int rowCount) {
        int lastRow = Math.max(0, rowCount - 1);
        sheet.setAutoFilter(new CellRangeAddress(0, lastRow, 0, columnCount - 1));
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            int width = Math.min(sheet.getColumnWidth(column) + 512, 40 * 256);
            sheet.setColumnWidth(column, width);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDateTimeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper()
                .createDataFormat()
                .getFormat("dd.mm.yyyy hh:mm"));
        return style;
    }

    private void setLong(Row row, int column, Long value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        } else {
            row.createCell(column);
        }
    }

    private void setDateTime(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }
}
