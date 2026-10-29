package ru.mirea.blogplatform.fx;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.fx.controller.MainController;
import ru.mirea.blogplatform.fx.controller.PostsController;
import ru.mirea.blogplatform.fx.form.CommentFormController;
import ru.mirea.blogplatform.fx.form.PostFormController;
import ru.mirea.blogplatform.fx.form.UserFormController;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.service.WorkspaceSnapshot;

@EnabledIf("hasDisplay")
class FxmlViewsTest {
    static boolean hasDisplay() { return !GraphicsEnvironment.isHeadless(); }

    @BeforeAll static void startJavaFx() {
        Platform.startup(() -> Platform.setImplicitExit(false));
    }

    @AfterAll static void stopJavaFx() { Platform.exit(); }

    @Test void loadsMainWindowAndAllFormsWithTheirControllers() throws Exception {
        FutureTask<Void> check = new FutureTask<>(() -> {
            try (AppContext context = new AppContext(new DatabaseManager("jdbc:postgresql://localhost/unused", "unused", ""))) {
                FXMLLoader main = new FXMLLoader(getClass().getResource("/fx/main.fxml"));
                Parent root = main.load();
                ((MainController) main.getController()).configure(context);
                assertNotNull(root.lookup("#tabs"));
                FXMLLoader post = new FXMLLoader(getClass().getResource("/fx/post-form.fxml"));
                post.load();
                ((PostFormController) post.getController()).configure(context, null);
                assertThrows(RuntimeException.class, () -> ((PostFormController) post.getController()).saveOperation());
                FXMLLoader user = new FXMLLoader(getClass().getResource("/fx/user-form.fxml"));
                user.load();
                ((UserFormController) user.getController()).configure(context, null);
                assertNotNull(((UserFormController) user.getController()).saveOperation());
                FXMLLoader comment = new FXMLLoader(getClass().getResource("/fx/comment-form.fxml"));
                comment.load();
                ((CommentFormController) comment.getController()).configure(context, null);
                assertThrows(RuntimeException.class, () -> ((CommentFormController) comment.getController()).saveOperation());
            }
            return null;
        });
        Platform.runLater(check);
        check.get(20, TimeUnit.SECONDS);
    }

    @Test @SuppressWarnings("unchecked")
    void observableTableCombinesSearchFiltersSortAndVisibleExport() throws Exception {
        FutureTask<Void> check = new FutureTask<>(() -> {
            try (AppContext context = new AppContext(new DatabaseManager("jdbc:postgresql://localhost/unused", "unused", ""))) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fx/posts.fxml"));
                Parent root = loader.load();
                PostsController controller = loader.getController();
                controller.configure(context);
                User author = new User(1L, "Тестовый автор", "fx@example.com");
                LocalDateTime date = LocalDateTime.of(2026, 10, 10, 12, 0);
                BlogPost first = new BlogPost(1L, 1L, "Бета", "beta", "Тестовый текст", PostStatus.DRAFT, date, null);
                BlogPost second = new BlogPost(2L, 1L, "Альфа", "alpha", "Тестовый текст", PostStatus.REVIEW, date.plusDays(1), null);
                context.snapshotProperty().set(new WorkspaceSnapshot(List.of(author), List.of(first, second), List.of()));
                TableView<BlogPost> table = (TableView<BlogPost>) root.lookup("#table");
                TextField search = (TextField) root.lookup("#searchField");
                ComboBox<PostStatus> status = (ComboBox<PostStatus>) root.lookup("#statusFilter");
                assertEquals(List.of(second, first), List.copyOf(table.getItems()));
                search.setText("тестовый автор");
                status.setValue(PostStatus.DRAFT);
                assertEquals(List.of(first), List.copyOf(table.getItems()));
                assertEquals(1, controller.exportTable().rows().size());
                assertEquals("Бета", controller.exportTable().rows().getFirst().getFirst());
                status.setValue(null);
                TableColumn<BlogPost, ?> title = table.getColumns().getFirst();
                title.setSortType(TableColumn.SortType.ASCENDING);
                table.getSortOrder().setAll(List.of(title));
                assertEquals(List.of(second, first), List.copyOf(table.getItems()));
                title.setSortType(TableColumn.SortType.DESCENDING);
                table.sort();
                assertEquals(List.of(first, second), List.copyOf(table.getItems()));
                search.setText("не существующий текст");
                assertTrue(table.getItems().isEmpty());
                assertTrue(controller.exportTable().rows().isEmpty());
            }
            return null;
        });
        Platform.runLater(check);
        check.get(20, TimeUnit.SECONDS);
    }
}
