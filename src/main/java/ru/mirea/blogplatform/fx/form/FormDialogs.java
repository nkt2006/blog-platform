package ru.mirea.blogplatform.fx.form;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.fx.util.Ui;

public final class FormDialogs {
    private FormDialogs() { }

    public static <C extends FormController> void show(Window owner, AppContext context, String title,
                                                       String resource, Class<C> type, Consumer<C> configure) {
        try {
            FXMLLoader loader = new FXMLLoader(FormDialogs.class.getResource("/fx/" + resource));
            Parent content = loader.load();
            C controller = type.cast(loader.getController());
            configure.accept(controller);
            Dialog<Void> dialog = new Dialog<>();
            dialog.initOwner(owner);
            dialog.setTitle(title);
            dialog.setHeaderText(title);
            dialog.setResizable(true);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getStylesheets().add(FormDialogs.class.getResource("/fx/blog.css").toExternalForm());
            ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
            Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
            Button cancel = (Button) dialog.getDialogPane().lookupButton(cancelType);
            save.getStyleClass().add("primary-button");
            save.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                try {
                    Callable<?> operation = controller.saveOperation();
                    controller.showError("");
                    save.setDisable(true);
                    cancel.setDisable(true);
                    content.setDisable(true);
                    context.tasks.run("Сохранение данных…", operation, result -> {
                        save.setDisable(false);
                        cancel.setDisable(false);
                        dialog.close();
                        context.refresh();
                    }, error -> {
                        save.setDisable(false);
                        cancel.setDisable(false);
                        content.setDisable(false);
                        controller.showError(Ui.message(error));
                    });
                } catch (RuntimeException error) {
                    controller.showError(Ui.message(error));
                }
            });
            dialog.setOnCloseRequest(event -> { if (context.tasks.busyProperty().get()) event.consume(); });
            dialog.showAndWait();
        } catch (IOException error) {
            Ui.error(owner, error);
        }
    }
}
