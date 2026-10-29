package ru.mirea.blogplatform.fx.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import ru.mirea.blogplatform.fx.form.CommentFormController;
import ru.mirea.blogplatform.fx.form.FormDialogs;
import ru.mirea.blogplatform.fx.util.Controls;
import ru.mirea.blogplatform.fx.util.Ui;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.CommentStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.service.ExportTable;

public class CommentsController extends TableController<Comment> {
    @FXML private ComboBox<CommentStatus> statusFilter;
    @FXML private ComboBox<BlogPost> postFilter;
    @FXML private ComboBox<User> authorFilter;
    @FXML private Button statusButton;

    @Override protected void setup() {
        var post = Controls.<Comment, String>column("Публикация", comment -> context.data().postTitle(comment.getPostId()), 240);
        var author = Controls.<Comment, String>column("Автор", comment -> context.data().authorName(comment.getAuthorId()), 190);
        var text = Controls.column("Текст", Comment::getText, 350);
        var status = Controls.<Comment, String>column("Статус", comment -> comment.getStatus().getDisplayName(), 135);
        var created = Controls.dateColumn("Создано", Comment::getCreatedAt);
        table.getColumns().setAll(List.of(post, author, text, status, created));
        created.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().setAll(List.of(created));
        statusFilter.getItems().setAll(CommentStatus.values());
        Controls.names(statusFilter, CommentStatus::getDisplayName);
        Controls.names(postFilter, BlogPost::getTitle);
        Controls.names(authorFilter, User::getName);
        statusFilter.valueProperty().addListener((a, b, c) -> applyFilters());
        postFilter.valueProperty().addListener((a, b, c) -> applyFilters());
        authorFilter.valueProperty().addListener((a, b, c) -> applyFilters());
        table.getSelectionModel().selectedItemProperty().addListener((a, b, comment) ->
                statusButton.setDisable(comment == null || comment.getStatus() != CommentStatus.PENDING));
        statusButton.setDisable(true);
    }

    @Override protected void updateData() {
        Controls.replace(postFilter, context.data().posts(), BlogPost::getId);
        Controls.replace(authorFilter, context.data().users(), User::getId);
        replaceRows(context.data().comments());
    }

    @Override @FXML protected void applyFilters() {
        if (context != null) filter(comment -> context.queries.matchesComment(comment, context.data(), searchField.getText(),
                statusFilter.getValue(), postFilter.getValue(), authorFilter.getValue()));
    }

    @Override @FXML protected void resetFilters() {
        searchField.clear(); statusFilter.setValue(null); postFilter.setValue(null); authorFilter.setValue(null);
    }

    @Override @FXML protected void add() { form(null); }
    @Override @FXML protected void edit() { if (selected() != null) form(selected()); }

    private void form(Comment comment) {
        FormDialogs.show(owner(), context, comment == null ? "Новый комментарий" : "Изменить комментарий",
                "comment-form.fxml", CommentFormController.class, controller -> controller.configure(context, comment));
    }

    @Override @FXML protected void delete() {
        Comment comment = selected();
        if (comment == null || !Ui.confirm(owner(), "Удалить выбранный комментарий?")) return;
        context.tasks.run("Удаление комментария…", () -> { context.comments.delete(comment.getId()); return null; },
                value -> context.refresh(), error -> Ui.error(owner(), error));
    }

    @FXML private void changeStatus() {
        Comment comment = selected();
        if (comment == null || comment.getStatus() != CommentStatus.PENDING) return;
        Ui.choose(owner(), "Модерация комментария", "Одобрить или отклонить комментарий?",
                List.of(CommentStatus.APPROVED.getDisplayName(), CommentStatus.REJECTED.getDisplayName()))
                .ifPresent(choice -> context.tasks.run("Модерация комментария…",
                        () -> context.comments.changeStatus(comment.getId(), CommentStatus.fromUserInput(choice)),
                        value -> context.refresh(), error -> Ui.error(owner(), error)));
    }

    @Override public ExportTable exportTable() { return context.exports.comments(visibleRows(), context.data()); }
}
