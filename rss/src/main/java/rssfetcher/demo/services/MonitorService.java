package rssfetcher.demo.services;

import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import rssfetcher.demo.entities.Article;
import rssfetcher.demo.entities.NewsSource;
import rssfetcher.demo.events.NewsSourceRepoChangeEvent;
import rssfetcher.demo.repositories.ArticleRepository;
import rssfetcher.demo.repositories.NewsSourceRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class MonitorService {
    protected final ArticleRepository articleRepository;
    protected final NewsSourceRepository newsSourceRepository;
    protected final List<NewsSource> newsSources;
    protected final RssFetcher rssFetcher;
    protected final TopicAssigner topicAssigner;

    public MonitorService(ArticleRepository articleRepository, NewsSourceRepository newsSourceRepository, RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        this.articleRepository = articleRepository;
        this.newsSourceRepository = newsSourceRepository;
        this.newsSources = newsSourceRepository.findAll();
        this.rssFetcher = rssFetcher;
        this.topicAssigner = topicAssigner;
    }

    public void startMonitoring() {
        var articles = monitor(newsSources);
        try {
            articleRepository.saveAll(articles);
        } catch (DataIntegrityViolationException ignored) {}
    }

    /**
     * This method ensures there are no unnecessary calls to the database
     */
    @EventListener
    public void handleNewsSourceRepoChangeEvent(NewsSourceRepoChangeEvent event) {
        newsSources.clear();
        newsSources.addAll(newsSourceRepository.findAll());
    }

    private List<Article> monitor(List<NewsSource> newsSources) {
        List<Article> articles = new ArrayList<>();
        for (NewsSource newsSource : newsSources) {
            List<Article> fetchedArticles = rssFetcher.fetchFrom(newsSource);
            fetchedArticles.forEach(topicAssigner::assignTopic);
            articles.addAll(fetchedArticles);
        }
        return articles;
    }

}
