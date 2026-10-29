package ru.mirea.blogplatform.fx.form;

import java.util.concurrent.Callable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public abstract class FormController {
    @FXML private Label errorLabel;
    public void showError(String message) { errorLabel.setText(message); }
    public abstract Callable<?> saveOperation();
}
