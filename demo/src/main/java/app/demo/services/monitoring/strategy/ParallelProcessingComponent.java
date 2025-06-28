package app.demo.services.monitoring.strategy;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.services.monitoring.RssFetcher;
import app.demo.services.monitoring.TopicAssigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParallelProcessingComponent {
    private final RssFetcher rssFetcher;
    private final TopicAssigner topicAssigner;


    @Async("myTaskExecutor")
    public CompletableFuture<List<Article>> fetchAndAssignAsync(NewsSource source) {
        log.info("Fetching articles from thread: {}", Thread.currentThread().getName());
        List<Article> articles = rssFetcher.fetchFrom(source);
        if (articles == null || articles.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        articles.parallelStream().forEach(topicAssigner::assignTopic);
        return CompletableFuture.completedFuture(articles);
    }
}
