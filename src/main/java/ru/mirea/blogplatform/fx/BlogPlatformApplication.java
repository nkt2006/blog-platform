package ru.mirea.blogplatform.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.fx.controller.MainController;
import ru.mirea.blogplatform.fx.util.Ui;

public class BlogPlatformApplication extends Application {
    private AppContext context;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fx/main.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 780);
        scene.getStylesheets().add(getClass().getResource("/fx/blog.css").toExternalForm());
        stage.setTitle("Блог-платформа · КР №2");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
        try {
            context = new AppContext(new DatabaseManager());
            MainController controller = loader.getController();
            controller.configure(context);
            Platform.runLater(context::refresh);
        } catch (RuntimeException error) {
            Ui.error(stage, error);
            // Неверный файл конфигурации не должен оставлять неработающее окно.
            stage.close();
        }
    }

    @Override public void stop() { if (context != null) context.close(); }

    public static void main(String[] args) { launch(args); }
}
