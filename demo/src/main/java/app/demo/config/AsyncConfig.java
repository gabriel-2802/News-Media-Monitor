package app.demo.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * configuration class that sets up asynchronous execution support in the application.
 * <p>
 * it enables Spring's asynchronous method execution and defines a custom thread pool
 * executor using properties loaded from {@link AsyncProperties}.
 * </p>
 */
@Configuration
@EnableAsync
@AllArgsConstructor
public class AsyncConfig {
    private final AsyncProperties asyncProperties;

    @Bean
    public Executor myTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getQueueCapacity());
        executor.setThreadNamePrefix("MyAsync-");
        executor.initialize();
        return executor;
    }
}
