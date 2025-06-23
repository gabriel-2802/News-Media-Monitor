package app.demo.config;

import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import app.demo.services.monitoring.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitorServiceConfig {

    @Bean
    public AbstractMonitorService monitorService(@Value("${monitoring.strategy}") String strategy,
                                                 RssFetcher rssFetcher, ArticleRepository articleRepository,
                                                 NewsSourceRepository newsSourceRepository, TopicAssigner topicAssigner) {
        return switch (strategy.toLowerCase()) {
            case "async" -> new AsyncMonitorService(rssFetcher, articleRepository, newsSourceRepository, topicAssigner);
            case "single-threaded" -> new SingleThreadedMonitorService(rssFetcher, articleRepository, newsSourceRepository, topicAssigner);
            default -> throw new IllegalArgumentException("Unknown monitoring.strategy: " + strategy);
        };
    }
}
