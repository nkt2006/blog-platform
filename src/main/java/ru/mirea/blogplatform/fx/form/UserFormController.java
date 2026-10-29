package ru.mirea.blogplatform.fx.form;

import java.util.concurrent.Callable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.fx.util.Controls;
import ru.mirea.blogplatform.model.User;
import ru.mirea.blogplatform.model.UserRole;

public class UserFormController extends FormController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<UserRole> roleBox;
    private AppContext context;
    private User current;

    public void configure(AppContext context, User user) {
        this.context = context;
        current = user;
        roleBox.getItems().setAll(UserRole.values());
        Controls.names(roleBox, UserRole::getDisplayName);
        roleBox.setValue(user == null ? UserRole.AUTHOR : user.getRole());
        if (user != null) {
            nameField.setText(user.getName());
            emailField.setText(user.getEmail());
        }
    }

    @Override public Callable<?> saveOperation() {
        String name = nameField.getText();
        String email = emailField.getText();
        UserRole role = roleBox.getValue();
        return current == null ? () -> context.users.create(name, email, role)
                : () -> context.users.update(current.getId(), name, email, role);
    }
}
