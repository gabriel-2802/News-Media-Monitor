package app.demo.services.monitoring;

import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class MonitorService {
    protected final RssFetcher rssFetcher;
    protected final ArticleRepository articleRepository;
    protected final NewsSourceRepository newsSourceRepository;
    protected final TopicAssigner topicAssigner;

    public abstract void startMonitoring();
}
