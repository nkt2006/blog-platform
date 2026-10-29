package ru.mirea.blogplatform.fx.controller;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import ru.mirea.blogplatform.fx.form.FormDialogs;
import ru.mirea.blogplatform.fx.form.UserFormController;
import ru.mirea.blogplatform.fx.util.Controls;
import ru.mirea.blogplatform.fx.util.Ui;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;
import ru.mirea.blogplatform.service.ExportTable;

public class UsersController extends TableController<User> {
    @FXML private ComboBox<UserRole> roleFilter;

    @Override protected void setup() {
        var name = Controls.column("Имя автора", User::getName, 320);
        var email = Controls.column("Email", User::getEmail, 340);
        var role = Controls.<User, String>column("Роль", user -> user.getRole().getDisplayName(), 160);
        table.getColumns().setAll(List.of(name, email, role));
        table.getSortOrder().setAll(List.of(name));
        roleFilter.getItems().setAll(UserRole.values());
        Controls.names(roleFilter, UserRole::getDisplayName);
        roleFilter.valueProperty().addListener((a, b, c) -> applyFilters());
    }

    @Override protected void updateData() { replaceRows(context.data().users()); }

    @Override @FXML protected void applyFilters() {
        if (context != null) filter(user -> context.queries.matchesUser(user, searchField.getText(), roleFilter.getValue()));
    }

    @Override @FXML protected void resetFilters() { searchField.clear(); roleFilter.setValue(null); }
    @Override @FXML protected void add() { form(null); }
    @Override @FXML protected void edit() { if (selected() != null) form(selected()); }

    private void form(User user) {
        FormDialogs.show(owner(), context, user == null ? "Новый автор" : "Изменить автора",
                "user-form.fxml", UserFormController.class, controller -> controller.configure(context, user));
    }

    @Override @FXML protected void delete() {
        User user = selected();
        if (user == null || !Ui.confirm(owner(), "Удалить автора «" + user.getName() + "»?\nАвтора с публикациями или комментариями удалить нельзя.")) return;
        context.tasks.run("Удаление автора…", () -> { context.users.delete(user.getId()); return null; },
                value -> context.refresh(), error -> Ui.error(owner(), error));
    }

    @Override public ExportTable exportTable() { return context.exports.users(visibleRows()); }
}
