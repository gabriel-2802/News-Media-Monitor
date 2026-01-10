package rssfetcher.demo.services.engines;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rssfetcher.demo.entities.Article;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Classifier engine that delegates topic prediction to an external HTTP API
 */
@Component
@Slf4j
public class CustomApiClassifierEngine implements ClassificationEngine {
    private static final String API_URL = "http://localhost:8000/classify";
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String classify(Article article, List<String> topics) {
        try {
            // the default topic is first by convention
            String defaultTopic = topics.getFirst();

            // JSON payload
            Map<String, String> requestBody = Map.of(
                    "text", article.getTitle() + " " + article.getSummary(),
                    "default_topic", defaultTopic
            );

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // send request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ClassificationResponse responseBody = objectMapper.readValue(response.body(), ClassificationResponse.class);
                return responseBody.topic();

            } else {
                log.error("Failed to classify article. Status: {}, Payload: {}, err: {}",
                        response.statusCode(),
                        json,
                        response.body());
                log.error("Sent request is {}", request);

            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to classify article due to connection ", e);
        } catch (Exception e) {
            log.error("Unexpected error during classification: {}", e.getMessage());
        }

        return null;
    }

    record ClassificationResponse(String topic) {}
}
