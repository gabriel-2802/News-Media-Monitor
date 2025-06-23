package app.demo.services.monitoring;

import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
public abstract class AbstractMonitorService {
    protected final RssFetcher rssFetcher;
    protected final ArticleRepository articleRepository;
    protected final NewsSourceRepository newsSourceRepository;
    protected final TopicAssigner topicAssigner;

    public abstract void startMonitoring();
}
