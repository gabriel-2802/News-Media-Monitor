package app.demo.config;

import app.demo.services.monitoring.engines.ClassificationEngine;
import app.demo.services.monitoring.engines.CustomApiClassifierEngine;
import app.demo.services.monitoring.engines.HuggingFaceClassifierEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * configuration class that provides a {@link ClassificationEngine} bean
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
    ClassificationEngine classifierEngine(@Value("${monitoring.classification-engine}") String engineType) {
        return switch (engineType) {
            case "custom" -> new CustomApiClassifierEngine();
            case "hf" -> new HuggingFaceClassifierEngine();
            default -> throw new IllegalArgumentException("Unknown classification engine type: " + engineType);
        };
    }
}
