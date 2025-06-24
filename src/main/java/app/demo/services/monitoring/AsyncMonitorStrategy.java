package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncMonitorStrategy extends MonitorStrategy {
    private final ParallelProcessingService parallelProcService;

    public AsyncMonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner, ParallelProcessingService parallelProcService) {
        super(rssFetcher, topicAssigner);
        this.parallelProcService = parallelProcService;
    }

    @Override
    public List<Article> monitor(List<NewsSource> newsSources) {
        List<CompletableFuture<List<Article>>> futures = new ArrayList<>();

        for (var source : newsSources) {
            futures.add(parallelProcService.fetchAndAssignAsync(source));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }
}
