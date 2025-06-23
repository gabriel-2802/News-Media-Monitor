package app.demo.services.monitoring;

import app.demo.entities.NewsSource;
import app.demo.events.NewsSourceRepoChangeEvent;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

import java.util.List;

@RequiredArgsConstructor
public abstract class MonitorService {
    protected final RssFetcher rssFetcher;
    protected final ArticleRepository articleRepository;
    protected final NewsSourceRepository newsSourceRepository;
    protected final TopicAssigner topicAssigner;
    protected final List<NewsSource> newsSources = newsSourceRepository.findAll();

    public abstract void startMonitoring();

    /**
     * This method ensures there are no unnecessary calls to the database
     */
    @EventListener
    public void handleNewsSourceRepoChangeEvent(NewsSourceRepoChangeEvent event) {
        newsSources.clear();
        newsSources.addAll(newsSourceRepository.findAll());
    }
}
