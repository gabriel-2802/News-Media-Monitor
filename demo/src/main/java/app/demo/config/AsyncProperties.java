package app.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * configuration properties class that holds async executor settings.
 *
 * <p>
 * binds properties with the prefix {@code monitoring.async} from the application's configuration file
 * (e.g. application.yml or application.properties).
 * </p>
 */
@Component
@Data
@ConfigurationProperties(prefix = "monitoring.async")
public class AsyncProperties {
    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
}
