package ru.mirea.blogplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.blogplatform.model.User;

class TableExportServiceTest {
    @TempDir Path directory;
    private final TableExportService service = new TableExportService();

    @Test void exportsOnlySelectedRowsToExcel() throws Exception {
        ExportTable data = service.users(List.of(new User(1L, "Никита", "nikita@example.com")));
        Path path = service.export(data, directory.resolve("selected.xlsx"));
        try (var input = Files.newInputStream(path); var workbook = new XSSFWorkbook(input)) {
            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals(1, workbook.getSheet("Авторы").getLastRowNum());
            assertEquals("Никита", workbook.getSheet("Авторы").getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test void csvPreservesQuotesLineBreaksAndRussianText() throws Exception {
        ExportTable data = new ExportTable("Данные", List.of("Поле"), List.of(List.of("Текст; \"кавычки\"\nновая строка"), List.of("=1+1")));
        Path path = service.export(data, directory.resolve("selected.csv"));
        String csv = Files.readString(path);
        assertTrue(csv.startsWith("\ufeff"));
        assertTrue(csv.contains("\"Текст; \"\"кавычки\"\"\nновая строка\""));
        assertTrue(csv.contains("\"'=1+1\""));
        assertThrows(IllegalArgumentException.class, () -> service.export(data, directory.resolve("file.txt")));
    }
}
