package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service implementation that triggers article clustering via an external AI service.
 * Sends an HTTP POST request to the configured clustering endpoint.
 * Called by WorkerService after all sources have been fetched.
 */
@Service
@Slf4j
public class ClusterService {
    private final String clusteringUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public ClusterService(@Value("${clustering.api.url:http://localhost:8000/cluster}") String clusteringUrl) {
        this.clusteringUrl = clusteringUrl;
    }

    /**
     * Triggers clustering of articles. Should be called only once per fetch cycle.
     */
    public void cluster() {
        log.info("Triggering article clustering...");
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    clusteringUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Clustering completed successfully. Response: {}", response.getBody());
            } else {
                log.error("Clustering failed with status code: {} and body: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error during clustering: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }
    }
}



