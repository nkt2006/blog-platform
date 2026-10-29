package ru.mirea.blogplatform.service;

import java.util.EnumMap;
import java.util.Map;
import ru.mirea.blogplatform.model.BlogPost;
import ru.mirea.blogplatform.model.PostStatus;

public class StatisticsService {
    public BlogStatistics calculate(WorkspaceSnapshot data) {
        long published = data.posts().stream().filter(post -> post.getStatus() == PostStatus.PUBLISHED).count();
        long drafts = data.posts().stream().filter(post -> post.getStatus() == PostStatus.DRAFT).count();
        long authors = data.posts().stream().map(BlogPost::getAuthorId).distinct().count();
        double average = data.posts().stream().mapToInt(post -> post.getContent().length()).average().orElse(0);
        return new BlogStatistics(data.posts().size(), published, drafts, authors, average);
    }

    public Map<PostStatus, Long> countByStatus(WorkspaceSnapshot data) {
        Map<PostStatus, Long> counts = new EnumMap<>(PostStatus.class);
        for (PostStatus status : PostStatus.values()) {
            counts.put(status, data.posts().stream().filter(post -> post.getStatus() == status).count());
        }
        return counts;
    }
}
