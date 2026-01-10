package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rssfetcher.demo.entities.Article;
import rssfetcher.demo.entities.NewsSource;
import rssfetcher.demo.repositories.ArticleRepository;
import rssfetcher.demo.repositories.NewsSourceRepository;
import rssfetcher.demo.services.monitor.RssFetcher;
import rssfetcher.demo.services.monitor.TopicAssigner;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MonitorService {
    protected final ArticleRepository articleRepository;
    protected final NewsSourceRepository newsSourceRepository;
    protected final RssFetcher rssFetcher;
    protected final TopicAssigner topicAssigner;

    public MonitorService(ArticleRepository articleRepository, NewsSourceRepository newsSourceRepository, RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        this.articleRepository = articleRepository;
        this.newsSourceRepository = newsSourceRepository;
        this.rssFetcher = rssFetcher;
        this.topicAssigner = topicAssigner;
    }

    @Transactional
    public void processRssUrl(String rssUrl) {
        Optional<NewsSource> ns = newsSourceRepository.findByRssUrl(rssUrl);
        if (ns.isEmpty()) {
            return;
        }

        List<Article> fetchedArticles = rssFetcher.fetchFrom(ns.get());
        fetchedArticles.forEach(topicAssigner::assignTopic);

        try {
            articleRepository.saveAll(fetchedArticles);
        } catch (DataIntegrityViolationException ignored) {
            log.error("Something went wrong when saving articles from RSS URL: {}", rssUrl);
        }
    }
}
