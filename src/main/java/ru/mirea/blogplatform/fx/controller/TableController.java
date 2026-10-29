package ru.mirea.blogplatform.fx.controller;

import java.util.List;
import java.util.function.Predicate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.service.ExportTable;

/** Общая работа TableView без SQL и без дублирования обработки поиска/выбора. */
public abstract class TableController<T> {
    @FXML protected TableView<T> table;
    @FXML protected TextField searchField;
    @FXML protected Label countLabel;
    @FXML protected Label filterError;
    @FXML protected Button editButton;
    @FXML protected Button deleteButton;
    @FXML protected HBox actionBar;
    protected AppContext context;
    protected final ObservableList<T> rows = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(rows);
    private final SortedList<T> sorted = new SortedList<>(filtered);

    public final void configure(AppContext value) {
        context = value;
        actionBar.disableProperty().bind(context.tasks.busyProperty());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Записей нет. Измените фильтры или добавьте новую запись."));
        table.setItems(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        searchField.textProperty().addListener((observable, old, text) -> applyFilters());
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        setup();
        context.snapshotProperty().addListener((observable, old, data) -> updateData());
        updateData();
    }

    protected final void replaceRows(List<T> data) {
        rows.setAll(data);
        applyFilters();
    }

    protected final void filter(Predicate<T> predicate) {
        filtered.setPredicate(predicate);
        countLabel.setText("Показано " + filtered.size() + " из " + rows.size());
        filterError.setText("");
    }

    protected final void invalidFilter(String message) {
        filtered.setPredicate(value -> false);
        countLabel.setText("Исправьте фильтр");
        filterError.setText(message);
    }

    protected Window owner() { return table.getScene().getWindow(); }
    protected T selected() { return table.getSelectionModel().getSelectedItem(); }
    protected List<T> visibleRows() { return List.copyOf(table.getItems()); }
    protected abstract void setup();
    protected abstract void updateData();
    @FXML protected abstract void applyFilters();
    @FXML protected abstract void resetFilters();
    @FXML protected abstract void add();
    @FXML protected abstract void edit();
    @FXML protected abstract void delete();
    public abstract ExportTable exportTable();
}
