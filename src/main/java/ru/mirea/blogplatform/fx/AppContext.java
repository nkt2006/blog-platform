package ru.mirea.blogplatform.fx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import ru.mirea.blogplatform.config.DatabaseManager;
import ru.mirea.blogplatform.fx.util.Ui;
import ru.mirea.blogplatform.repository.BlogPostRepository;
import ru.mirea.blogplatform.repository.CommentRepository;
import ru.mirea.blogplatform.repository.UserRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcBlogPostRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcCommentRepository;
import ru.mirea.blogplatform.repository.jdbc.JdbcUserRepository;
import ru.mirea.blogplatform.service.BlogPostService;
import ru.mirea.blogplatform.service.CatalogQueryService;
import ru.mirea.blogplatform.service.CommentService;
import ru.mirea.blogplatform.service.TableExportService;
import ru.mirea.blogplatform.service.UserService;
import ru.mirea.blogplatform.service.WorkspaceSnapshot;

public final class AppContext implements AutoCloseable {
    public final FxTasks tasks = new FxTasks();
    public final BlogPostService posts;
    public final UserService users;
    public final CommentService comments;
    public final CatalogQueryService queries = new CatalogQueryService();
    public final TableExportService exports = new TableExportService();
    private final BlogPostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ObjectProperty<WorkspaceSnapshot> snapshot = new SimpleObjectProperty<>(WorkspaceSnapshot.empty());

    public AppContext(DatabaseManager database) {
        postRepository = new JdbcBlogPostRepository(database);
        userRepository = new JdbcUserRepository(database);
        commentRepository = new JdbcCommentRepository(database);
        posts = new BlogPostService(postRepository, userRepository, commentRepository);
        users = new UserService(userRepository);
        comments = new CommentService(commentRepository, postRepository, userRepository);
    }

    public WorkspaceSnapshot data() { return snapshot.get(); }
    public ObjectProperty<WorkspaceSnapshot> snapshotProperty() { return snapshot; }

    public void refresh() {
        tasks.run("Загрузка публикаций, авторов и комментариев…",
                () -> new WorkspaceSnapshot(userRepository.findAll(), postRepository.findAll(), commentRepository.findAll()),
                snapshot::set, error -> Ui.error(null, error));
    }

    @Override public void close() { tasks.close(); }
}
