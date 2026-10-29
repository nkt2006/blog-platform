package ru.mirea.blogplatform.fx.form;

import java.util.concurrent.Callable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import ru.mirea.blogplatform.exception.CommentException;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.fx.util.Controls;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.Comment;
import ru.mirea.blogplatform.model.User;

public class CommentFormController extends FormController {
    @FXML private ComboBox<BlogPost> postBox;
    @FXML private ComboBox<User> authorBox;
    @FXML private TextArea textField;
    @FXML private Label statusLabel;
    private AppContext context;
    private Comment current;

    public void configure(AppContext context, Comment comment) {
        this.context = context;
        current = comment;
        postBox.getItems().setAll(context.data().posts());
        authorBox.getItems().setAll(context.data().users());
        Controls.names(postBox, post -> post.getTitle() + " · " + post.getSlug());
        Controls.names(authorBox, user -> user.getName() + " · " + user.getEmail());
        statusLabel.setText(comment == null ? "Новый комментарий будет отправлен на проверку." : "Статус: " + comment.getStatus().getDisplayName());
        if (comment != null) {
            textField.setText(comment.getText());
            postBox.setValue(context.data().posts().stream().filter(post -> post.getId().equals(comment.getPostId())).findFirst().orElse(null));
            authorBox.setValue(context.data().users().stream().filter(user -> user.getId().equals(comment.getAuthorId())).findFirst().orElse(null));
            postBox.setDisable(true);
            authorBox.setDisable(true);
        }
    }

    @Override public Callable<?> saveOperation() {
        String text = textField.getText();
        if (current != null) return () -> context.comments.update(current.getId(), text);
        BlogPost post = postBox.getValue();
        User author = authorBox.getValue();
        if (post == null) throw new CommentException("Выберите публикацию.");
        if (author == null) throw new CommentException("Выберите автора комментария.");
        return () -> context.comments.create(post.getId(), author.getId(), text);
    }
}
