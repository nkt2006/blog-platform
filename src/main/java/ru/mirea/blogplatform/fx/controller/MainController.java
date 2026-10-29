package ru.mirea.blogplatform.fx.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.fx.util.Ui;
import ru.mirea.blogplatform.service.ExportTable;

public class MainController {
    @FXML private PostsController postsViewController;
    @FXML private UsersController usersViewController;
    @FXML private CommentsController commentsViewController;
    @FXML private StatisticsController statisticsViewController;
    @FXML private TabPane tabs;
    @FXML private Tab statisticsTab;
    @FXML private Button exportButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progress;
    @FXML private Label statusLabel;
    private AppContext context;

    public void configure(AppContext context) {
        this.context = context;
        postsViewController.configure(context);
        usersViewController.configure(context);
        commentsViewController.configure(context);
        statisticsViewController.configure(context);
        progress.visibleProperty().bind(context.tasks.busyProperty());
        statusLabel.textProperty().bind(context.tasks.messageProperty());
        refreshButton.disableProperty().bind(context.tasks.busyProperty());
        exportButton.disableProperty().bind(context.tasks.busyProperty()
                .or(tabs.getSelectionModel().selectedItemProperty().isEqualTo(statisticsTab)));
    }

    @FXML private void refresh() { if (context != null) context.refresh(); }

    @FXML private void export() {
        ExportTable data = switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> postsViewController.exportTable();
            case 1 -> usersViewController.exportTable();
            case 2 -> commentsViewController.exportTable();
            default -> throw new IllegalStateException("Выберите таблицу для экспорта.");
        };
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт текущего списка — " + data.title());
        FileChooser.ExtensionFilter excel = new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter csv = new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv");
        chooser.getExtensionFilters().addAll(excel, csv);
        chooser.setInitialFileName("blog-" + switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> "posts"; case 1 -> "authors"; default -> "comments";
        });
        File file = chooser.showSaveDialog(tabs.getScene().getWindow());
        if (file == null) return;
        String path = file.getPath();
        String lower = path.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".csv")) {
            path += chooser.getSelectedExtensionFilter() == csv ? ".csv" : ".xlsx";
        }
        Path destination = Path.of(path);
        // Расширение может быть добавлено после FileChooser: проверяем уже итоговый путь.
        if (Files.exists(destination) && !Ui.confirm(tabs.getScene().getWindow(), "Перезаписать файл?",
                "Файл уже существует:\n" + destination + "\nЗаменить его новым экспортом?")) return;
        context.tasks.run("Экспорт текущего списка…", () -> context.exports.export(data, destination),
                saved -> Ui.info(tabs.getScene().getWindow(), "Экспорт завершён",
                        "Сохранено строк: " + data.rows().size() + "\n" + saved),
                error -> Ui.error(tabs.getScene().getWindow(), error));
    }
}
