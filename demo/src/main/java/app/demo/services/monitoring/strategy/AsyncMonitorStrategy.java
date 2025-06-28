package app.demo.services.monitoring.strategy;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.services.monitoring.RssFetcher;
import app.demo.services.monitoring.TopicAssigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * asynchronous implementation of {@link MonitorStrategy} that fetches and assigns topics
 * to articles in parallel using {@link ParallelProcessingComponent}.
 * <p>
 * this strategy improves performance when monitoring multiple news sources by
 * leveraging asynchronous tasks and {@link CompletableFuture} for concurrent execution.
 */
@Component
@Slf4j
public class AsyncMonitorStrategy extends MonitorStrategy {
    private final ParallelProcessingComponent parallelProcService;

    public AsyncMonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner, ParallelProcessingComponent parallelProcService) {
        super(rssFetcher, topicAssigner);
        this.parallelProcService = parallelProcService;
    }

    @Override
    public List<Article> monitor(List<NewsSource> newsSources) {
        List<CompletableFuture<List<Article>>> futures = new ArrayList<>();

        for (var source : newsSources) {
            futures.add(parallelProcService.fetchAndAssignAsync(source)
                    .exceptionally(ex -> {
                        log.error("Error fetching articles from source: {}", source.getName(), ex);
                        return List.of();
                    }));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }
}
