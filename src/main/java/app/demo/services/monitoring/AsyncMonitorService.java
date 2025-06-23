package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
    * MonitorService is responsible for monitoring articles from various news sources asynchronously.
 */
@Service
public class AsyncMonitorService extends MonitorService {
    private final ParallelProcessingService parallelProcService;

    public AsyncMonitorService(RssFetcher rssFetcher, ArticleRepository articleRepository,
                               NewsSourceRepository newsSourceRepository, TopicAssigner topicAssigner, ParallelProcessingService parallelProcService) {
        super(rssFetcher, articleRepository, newsSourceRepository, topicAssigner);
        this.parallelProcService = parallelProcService;
    }

    /**
     * Starts monitoring articles from all registered news sources.
     * It fetches articles asynchronously and assigns topics to each article.
     * Finally, it saves all fetched articles to the repository.
     */
    @Override
    public void startMonitoring() {
        List<CompletableFuture<List<Article>>> futures = new ArrayList<>();

        for (var source : newsSources) {
            futures.add(parallelProcService.fetchAndAssignAsync(source));
        }

        List<Article> allArticles = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        try {
            articleRepository.saveAll(allArticles);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

}
