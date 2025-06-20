package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import app.demo.security.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;

/*
 * HuggingFaceClassifierEngine.java
 * This class implements the ClassifierEngine interface using Hugging Face's DistilBART model for text classification.
 * It sends a request to the Hugging Face API with the article content and receives the predicted label.
 */
@Component
@Slf4j
public class HuggingFaceClassifierEngine implements ClassifierEngine {

    private static final String API_URL = "https://api-inference.huggingface.co/models/valhalla/distilbart-mnli-12-1";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String classify(Article article, Set<String> labels) {
        String text = article.getTitle() + " " + article.getContent();

        try {
            // Format request JSON using ObjectMapper
            String requestJson = mapper.writeValueAsString(
                    new HFRequest(text, List.copyOf(labels))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + Constants.HF_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());

            return root.get("labels").get(0).asText();

        } catch (Exception e) {
            log.error("Classification failed: {}", e.getMessage());
            return null;
        }
    }

   /*
    * HFRequest.java
    * This record represents the request body sent to the Hugging Face API.
    */
    record HFRequest(String inputs, Parameters parameters) {
        HFRequest(String inputs, List<String> labels) {
            this(inputs, new Parameters(labels));
        }
    }

    record Parameters(List<String> candidate_labels) {}
}
