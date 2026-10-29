package ru.mirea.blogplatform.fx;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDBC и экспорт выполняются в фоновом потоке, callbacks — в JavaFX Application Thread. */
public final class FxTasks implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FxTasks.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(work -> {
        Thread thread = new Thread(work, "blog-database");
        thread.setDaemon(true);
        return thread;
    });
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final StringProperty message = new SimpleStringProperty("Подключение к базе…");

    public BooleanProperty busyProperty() { return busy; }
    public StringProperty messageProperty() { return message; }

    public <T> void run(String title, Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        message.set(title);
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> {
            busy.set(false);
            message.set("Готово");
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busy.set(false);
            message.set("Операция не выполнена. Исправьте ошибку и повторите.");
            LOG.error(title, task.getException());
            failure.accept(task.getException());
        });
        executor.submit(task);
    }

    @Override public void close() { executor.shutdownNow(); }
}
