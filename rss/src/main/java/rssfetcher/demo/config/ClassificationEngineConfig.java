package rssfetcher.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rssfetcher.demo.services.engines.ClassificationEngine;
import rssfetcher.demo.services.engines.CustomApiClassifierEngine;
import rssfetcher.demo.services.engines.HuggingFaceClassifierEngine;

/**
 * Configuration class that provides a {@link ClassificationEngine} bean
 * based on the application property {@code monitoring.classification-engine}.
 *
 * <p>
 * it supports dynamic selection between different engine implementations,
 * such as {@link CustomApiClassifierEngine} and {@link HuggingFaceClassifierEngine}.
 * </p>
 */
@Configuration
public class ClassificationEngineConfig {
    @Bean
    ClassificationEngine classifierEngine(
            @Value("${monitoring.classification-engine}") String engineType,
            @Value("${classification.api.url:http://localhost:8000/classify}") String apiUrl) {
        return new ClassificationEngineFactory().getEngine(engineType, apiUrl);
    }

    static class ClassificationEngineFactory {
        public ClassificationEngine getEngine(String engineType, String apiUrl) {
            return switch (engineType) {
                case "custom" -> new CustomApiClassifierEngine(apiUrl);
                case "hf" -> new HuggingFaceClassifierEngine();
                default -> throw new IllegalArgumentException("Unknown classification engine type: " + engineType);
            };
        }
    }
}
