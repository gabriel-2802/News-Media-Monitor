package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
    * MonitorService is responsible for monitoring articles from various news sources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorService {
    private final RssFetcher rssFetcher;
    private final ArticleRepository articleRepository;
    private final NewsSourceRepository newsSourceRepository;
    private final TopicAssigner topicAssigner;

    public void startMonitoring() {
        // Logic to start monitoring articles
        // This could involve initializing components like RssFetcher, ArticleClusterer, and TopicAssigner
        List<Article> articles = new ArrayList<>();
        List<NewsSource> newsSources = newsSourceRepository.findAll();
        for (NewsSource newsSource : newsSources) {
            List<Article> fetchedArticles = rssFetcher.fetchFrom(newsSource);
            fetchedArticles.forEach(topicAssigner::assignTopic);
            articles.addAll(fetchedArticles);
        }
        articleRepository.saveAll(articles);
    }
}
