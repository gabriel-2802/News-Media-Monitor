package app.demo.config;

import app.demo.services.monitoring.*;
import app.demo.services.monitoring.strategy.AsyncMonitorStrategy;
import app.demo.services.monitoring.strategy.MonitorStrategy;
import app.demo.services.monitoring.strategy.ParallelProcessingComponent;
import app.demo.services.monitoring.strategy.SequentialMonitorStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitorStrategyConfig {

    @Bean
    public MonitorStrategy monitorStrategy(@Value("${monitoring.strategy}") String strategy,
                                           RssFetcher rssFetcher, TopicAssigner topicAssigner, ParallelProcessingComponent parallelProcessingService) {
        return switch (strategy.toLowerCase()) {
            case "async" -> new AsyncMonitorStrategy(rssFetcher, topicAssigner, parallelProcessingService);
            case "single-threaded" -> new SequentialMonitorStrategy(rssFetcher, topicAssigner);
            default -> throw new IllegalArgumentException("Unknown monitoring.strategy: " + strategy);
        };
    }
}
