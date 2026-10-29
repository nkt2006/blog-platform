package ru.mirea.blogplatform.fx.util;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;

public final class Controls {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private Controls() { }

    public static <T> void replace(ComboBox<T> combo, List<T> values, Function<T, Long> id) {
        Long selected = combo.getValue() == null ? null : id.apply(combo.getValue());
        combo.getItems().setAll(values);
        combo.setValue(values.stream().filter(value -> id.apply(value).equals(selected)).findFirst().orElse(null));
    }

    public static void dates(DatePicker picker) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate value) { return value == null ? "" : format.format(value); }
            @Override public LocalDate fromString(String value) {
                return value == null || value.isBlank() ? null : LocalDate.parse(value.strip(), format);
            }
        });
        picker.setEditable(false);
        picker.setPromptText("дд.мм.гггг");
    }

    public static <T, V> TableColumn<T, V> column(String title, Function<T, V> getter, double width) {
        TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setMinWidth(80);
        return column;
    }

    public static <T> TableColumn<T, LocalDateTime> dateColumn(String title, Function<T, LocalDateTime> getter) {
        TableColumn<T, LocalDateTime> column = column(title, getter, 155);
        column.setCellFactory(value -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "" : DATE.format(date));
            }
        });
        return column;
    }

    public static <T> void names(ComboBox<T> combo, Function<T, String> display) {
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : display.apply(value); }
            @Override public T fromString(String value) { return null; }
        });
    }
}
