package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
    * MonitorService is responsible for monitoring articles from various news sources asynchronously.
 */
@Service
public class AsyncMonitorService extends AbstractMonitorService {

    public AsyncMonitorService(RssFetcher rssFetcher, ArticleRepository articleRepository, NewsSourceRepository newsSourceRepository, TopicAssigner topicAssigner) {
        super(rssFetcher, articleRepository, newsSourceRepository, topicAssigner);
    }

    /**
     * Starts monitoring articles from all registered news sources.
     * It fetches articles asynchronously and assigns topics to each article.
     * Finally, it saves all fetched articles to the repository.
     */
    @Override
    public void startMonitoring() {

    }

}
