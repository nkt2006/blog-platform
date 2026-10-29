package ru.mirea.blogplatform.fx.controller;

import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import ru.mirea.blogplatform.fx.AppContext;
import ru.mirea.blogplatform.model.PostStatus;
import ru.mirea.blogplatform.service.BlogStatistics;
import ru.mirea.blogplatform.service.StatisticsService;

public class StatisticsController {
    @FXML private Label totalValue;
    @FXML private Label publishedValue;
    @FXML private Label draftsValue;
    @FXML private Label authorsValue;
    @FXML private Label averageValue;
    @FXML private Label relatedLabel;
    @FXML private BarChart<String, Number> statusChart;
    private final StatisticsService service = new StatisticsService();

    public void configure(AppContext context) {
        context.snapshotProperty().addListener((a, b, c) -> update(context));
        update(context);
    }

    private void update(AppContext context) {
        BlogStatistics statistics = service.calculate(context.data());
        totalValue.setText(Long.toString(statistics.getTotalPosts()));
        publishedValue.setText(Long.toString(statistics.getPublishedPosts()));
        draftsValue.setText(Long.toString(statistics.getDraftPosts()));
        authorsValue.setText(Long.toString(statistics.getAuthorsWithPosts()));
        averageValue.setText(String.format(Locale.ROOT, "%.1f", statistics.getAverageContentLength()));
        relatedLabel.setText("Авторов в системе: " + context.data().users().size()
                + "   •   Комментариев: " + context.data().comments().size());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        var counts = service.countByStatus(context.data());
        for (PostStatus status : PostStatus.values()) {
            series.getData().add(new XYChart.Data<>(status.getDisplayName(), counts.get(status)));
        }
        statusChart.getData().setAll(java.util.List.of(series));
    }
}
