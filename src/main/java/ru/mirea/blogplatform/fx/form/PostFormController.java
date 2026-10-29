package ru.mirea.blogplatform.fx.form;

import java.util.concurrent.Callable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.mirea.blogplatform.exception.BusinessRuleException;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.fx.util.Controls;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.User;

public class PostFormController extends FormController {
    @FXML private TextField titleField;
    @FXML private TextField slugField;
    @FXML private TextArea contentField;
    @FXML private ComboBox<User> authorBox;
    @FXML private Label statusLabel;
    private AppContext context;
    private BlogPost current;

    public void configure(AppContext context, BlogPost post) {
        this.context = context;
        current = post;
        authorBox.getItems().setAll(context.data().users());
        Controls.names(authorBox, user -> user.getName() + " · " + user.getEmail());
        statusLabel.setText(post == null ? "Новая публикация сохранится как черновик." : "Статус: " + post.getStatus().getDisplayName());
        if (post != null) {
            titleField.setText(post.getTitle());
            slugField.setText(post.getSlug());
            contentField.setText(post.getContent());
            authorBox.setValue(context.data().users().stream().filter(user -> user.getId().equals(post.getAuthorId())).findFirst().orElse(null));
        }
    }

    @Override public Callable<?> saveOperation() {
        User author = authorBox.getValue();
        if (author == null) throw new BusinessRuleException("Выберите автора. Если список пуст, сначала добавьте автора на вкладке «Авторы».");
        long authorId = author.getId();
        String title = titleField.getText();
        String slug = slugField.getText();
        String content = contentField.getText();
        return current == null ? () -> context.posts.createPost(authorId, title, slug, content)
                : () -> context.posts.updatePost(current.getId(), authorId, title, slug, content);
    }
}
