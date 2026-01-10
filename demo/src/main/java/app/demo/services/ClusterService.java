package app.demo.services;

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
 */
@Service
@Slf4j
public class ClusterService {
    private final String clusteringUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public ClusterService(@Value("${clustering.api.url:http://localhost:8000/cluster}") String clusteringUrl) {
        this.clusteringUrl = clusteringUrl;
        log.info("ClusterServiceImpl1 initialized with clustering URL: {}", clusteringUrl);
    }

    public void cluster() {
        try {
            log.info("Sending clustering request to: {}", clusteringUrl);

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



