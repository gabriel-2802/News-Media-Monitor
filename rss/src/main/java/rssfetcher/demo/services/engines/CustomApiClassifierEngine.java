package rssfetcher.demo.services.engines;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import rssfetcher.demo.entities.Article;

import java.util.List;
import java.util.Map;

/**
 * Classifier engine that delegates topic prediction to an external HTTP API
 */
@Slf4j
public class CustomApiClassifierEngine implements ClassificationEngine {
    private final String apiUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public CustomApiClassifierEngine(String apiUrl) {
        this.apiUrl = apiUrl;
        log.info("CustomApiClassifierEngine initialized with API URL: {}", apiUrl);
    }

    @Override
    public String classify(Article article, List<String> topics) {
        try {
            log.info("Starting classification for article: {}", article.getTitle());
            log.info("Using API URL: {}", apiUrl);

            // the default topic is first by convention
            String defaultTopic = topics.getFirst();

            // JSON payload
            Map<String, String> requestBody = Map.of(
                    "text", article.getTitle() + " " + article.getSummary(),
                    "default_topic", defaultTopic
            );

            log.info("Request payload: {}", requestBody);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            log.info("Sending request to: {}", apiUrl);
            // Send POST request
            ResponseEntity<ClassificationResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    ClassificationResponse.class
            );

            log.info("Received response with status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String topic = response.getBody().topic();
                log.info("Classification successful: {}", topic);
                return topic;
            } else {
                log.error("Failed to classify article. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error during classification: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }

        return null;
    }

    record ClassificationResponse(String topic) {}
}
