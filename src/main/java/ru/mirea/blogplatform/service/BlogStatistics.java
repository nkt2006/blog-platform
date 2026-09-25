package ru.mirea.blogplatform.service;

public class BlogStatistics {
    private final long totalPosts;
    private final long publishedPosts;
    private final long draftPosts;
    private final long authorsWithPosts;
    private final double averageContentLength;

    public BlogStatistics(long totalPosts, long publishedPosts, long draftPosts,
                          long authorsWithPosts, double averageContentLength) {
        this.totalPosts = totalPosts;
        this.publishedPosts = publishedPosts;
        this.draftPosts = draftPosts;
        this.authorsWithPosts = authorsWithPosts;
        this.averageContentLength = averageContentLength;
    }

    public long getTotalPosts() {
        return totalPosts;
    }

    public long getPublishedPosts() {
        return publishedPosts;
    }

    public long getDraftPosts() {
        return draftPosts;
    }

    public long getAuthorsWithPosts() {
        return authorsWithPosts;
    }

    public double getAverageContentLength() {
        return averageContentLength;
    }
}
