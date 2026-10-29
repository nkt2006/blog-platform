package ru.mirea.blogplatform.fx.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import ru.mirea.blogplatform.exception.BlogPlatformException;
import ru.mirea.blogplatform.exception.DataExportException;

public final class Ui {
    private Ui() { }

    public static String message(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof SQLException sql) {
                if ("23505".equals(sql.getSQLState())) {
                    return "Такая запись уже существует. Проверьте уникальность email, адреса публикации или текста комментария.";
                }
                if ("23503".equals(sql.getSQLState())) {
                    return "Запись связана с другими данными. Сначала удалите связанные публикации или комментарии.";
                }
            }
            cause = cause.getCause();
        }
        if (error instanceof BlogPlatformException || error instanceof DataExportException || error instanceof IllegalArgumentException) {
            return error.getMessage();
        }
        return "Не удалось выполнить операцию. Проверьте подключение и повторите попытку.";
    }

    public static void error(Window owner, Throwable error) {
        Alert alert = alert(owner, Alert.AlertType.ERROR, "Ошибка", message(error));
        alert.showAndWait();
    }

    public static void info(Window owner, String title, String text) {
        alert(owner, Alert.AlertType.INFORMATION, title, text).showAndWait();
    }

    public static boolean confirm(Window owner, String text) {
        return confirm(owner, "Подтверждение удаления", text);
    }

    public static boolean confirm(Window owner, String title, String text) {
        return alert(owner, Alert.AlertType.CONFIRMATION, title, text)
                .showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    public static Optional<String> choose(Window owner, String title, String text, List<String> values) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(values.getFirst(), values);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(text);
        dialog.getDialogPane().getStylesheets().add(Ui.class.getResource("/fx/blog.css").toExternalForm());
        return dialog.showAndWait();
    }

    public static void details(Window owner, String title, String text) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        TextArea content = new TextArea(text);
        content.setEditable(false);
        content.setWrapText(true);
        content.setPrefSize(650, 380);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(Ui.class.getResource("/fx/blog.css").toExternalForm());
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private static Alert alert(Window owner, Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(text);
        alert.getDialogPane().setMinWidth(460);
        alert.getDialogPane().getStylesheets().add(Ui.class.getResource("/fx/blog.css").toExternalForm());
        return alert;
    }
}
