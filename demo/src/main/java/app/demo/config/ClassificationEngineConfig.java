package app.demo.config;

import app.demo.services.monitoring.engines.ClassificationEngine;
import app.demo.services.monitoring.engines.CustomApiClassifierEngine;
import app.demo.services.monitoring.engines.HuggingFaceClassifierEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
