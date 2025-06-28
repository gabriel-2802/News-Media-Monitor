package app.demo.services.monitoring;

import app.demo.entities.NewsSource;
import app.demo.events.NewsSourceRepoChangeEvent;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import app.demo.services.monitoring.strategy.MonitorStrategy;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitorService {
    protected final ArticleRepository articleRepository;
    protected final NewsSourceRepository newsSourceRepository;
    protected final List<NewsSource> newsSources;
    protected final MonitorStrategy monitorStrategy;

    public MonitorService(ArticleRepository articleRepository, NewsSourceRepository newsSourceRepository, MonitorStrategy monitorStrategy) {
        this.articleRepository = articleRepository;
        this.newsSourceRepository = newsSourceRepository;
        this.newsSources = newsSourceRepository.findAll();
        this.monitorStrategy = monitorStrategy;
    }

    public void startMonitoring() {
        var articles = monitorStrategy.monitor(newsSources);
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

}
