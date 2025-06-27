package app.demo.config;

import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import app.demo.services.monitoring.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitorStrategyConfig {

    @Bean
    public MonitorStrategy monitorStrategy(@Value("${monitoring.strategy}") String strategy,
                                           RssFetcher rssFetcher, TopicAssigner topicAssigner, ParallelProcessingService parallelProcessingService) {
        return switch (strategy.toLowerCase()) {
            case "async" -> new AsyncMonitorStrategy(rssFetcher, topicAssigner, parallelProcessingService);
            case "single-threaded" -> new SyncMonitorStrategy(rssFetcher, topicAssigner);
            default -> throw new IllegalArgumentException("Unknown monitoring.strategy: " + strategy);
        };
    }
}
