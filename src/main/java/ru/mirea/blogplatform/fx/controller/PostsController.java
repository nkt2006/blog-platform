package ru.mirea.blogplatform.fx.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import ru.mirea.blogplatform.fx.form.FormDialogs;
import ru.mirea.blogplatform.fx.form.PostFormController;
import ru.mirea.blogplatform.fx.util.Controls;
import ru.mirea.blogplatform.fx.util.Ui;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.service.ExportTable;

public class PostsController extends TableController<BlogPost> {
    @FXML private ComboBox<PostStatus> statusFilter;
    @FXML private ComboBox<User> authorFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Button statusButton;
    @FXML private Button detailsButton;

    @Override protected void setup() {
        var title = Controls.column("Заголовок", BlogPost::getTitle, 260);
        var author = Controls.<BlogPost, String>column("Автор", post -> context.data().authorName(post.getAuthorId()), 190);
        var status = Controls.<BlogPost, String>column("Статус", post -> post.getStatus().getDisplayName(), 130);
        var created = Controls.dateColumn("Создано", BlogPost::getCreatedAt);
        var published = Controls.dateColumn("Опубликовано", BlogPost::getPublishedAt);
        var slug = Controls.column("Адрес публикации", BlogPost::getSlug, 200);
        table.getColumns().setAll(List.of(title, author, status, created, published, slug));
        created.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().setAll(List.of(created));
        statusFilter.getItems().setAll(PostStatus.values());
        Controls.names(statusFilter, PostStatus::getDisplayName);
        Controls.names(authorFilter, user -> user.getName() + " · " + user.getEmail());
        Controls.dates(fromDate);
        Controls.dates(toDate);
        statusFilter.valueProperty().addListener((a, b, c) -> applyFilters());
        authorFilter.valueProperty().addListener((a, b, c) -> applyFilters());
        fromDate.valueProperty().addListener((a, b, c) -> applyFilters());
        toDate.valueProperty().addListener((a, b, c) -> applyFilters());
        detailsButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        table.getSelectionModel().selectedItemProperty().addListener((a, b, post) ->
                statusButton.setDisable(post == null || post.getStatus() == PostStatus.ARCHIVED));
        statusButton.setDisable(true);
    }

    @Override protected void updateData() {
        Controls.replace(authorFilter, context.data().users(), User::getId);
        replaceRows(context.data().posts());
    }

    @Override @FXML protected void applyFilters() {
        if (context == null) return;
        try {
            context.queries.validateDates(fromDate.getValue(), toDate.getValue());
            filter(post -> context.queries.matchesPost(post, context.data(), searchField.getText(),
                    statusFilter.getValue(), authorFilter.getValue(), fromDate.getValue(), toDate.getValue()));
        } catch (IllegalArgumentException | ru.mirea.blogplatform.exception.BusinessRuleException error) {
            invalidFilter(error.getMessage());
        }
    }

    @Override @FXML protected void resetFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        authorFilter.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    @Override @FXML protected void add() { form(null); }
    @Override @FXML protected void edit() { if (selected() != null) form(selected()); }

    private void form(BlogPost post) {
        FormDialogs.show(owner(), context, post == null ? "Новая публикация" : "Изменить публикацию",
                "post-form.fxml", PostFormController.class, controller -> controller.configure(context, post));
    }

    @Override @FXML protected void delete() {
        BlogPost post = selected();
        if (post == null || !Ui.confirm(owner(), "Удалить «" + post.getTitle() + "»?\nКомментарии этой публикации тоже будут удалены.")) return;
        context.tasks.run("Удаление публикации…", () -> { context.posts.deletePost(post.getId()); return null; },
                value -> context.refresh(), error -> Ui.error(owner(), error));
    }

    @FXML private void details() {
        BlogPost post = selected();
        if (post == null) return;
        Ui.details(owner(), post.getTitle(), "Автор: " + context.data().authorName(post.getAuthorId())
                + "\nСтатус: " + post.getStatus().getDisplayName() + "\nАдрес: " + post.getSlug()
                + "\nСоздано: " + Controls.DATE.format(post.getCreatedAt()) + "\n\n" + post.getContent());
    }

    @FXML private void changeStatus() {
        BlogPost post = selected();
        if (post == null || post.getStatus() == PostStatus.ARCHIVED) return;
        PostStatus next = switch (post.getStatus()) {
            case DRAFT -> PostStatus.REVIEW;
            case REVIEW -> PostStatus.PUBLISHED;
            case PUBLISHED -> PostStatus.ARCHIVED;
            case ARCHIVED -> throw new IllegalStateException();
        };
        Ui.choose(owner(), "Смена статуса", "Текущий статус: " + post.getStatus().getDisplayName(),
                List.of(next.getDisplayName())).ifPresent(choice -> context.tasks.run("Смена статуса…",
                () -> context.posts.changeStatus(post.getId(), next), value -> context.refresh(), error -> Ui.error(owner(), error)));
    }

    @Override public ExportTable exportTable() { return context.exports.posts(visibleRows(), context.data()); }
}
