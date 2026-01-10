package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

/**
 * Service implementation that triggers article clustering via an external AI service.
 * Sends an HTTP POST request to the configured clustering endpoint.
 */
@Service
@Slf4j
public class ClusterServiceV1 implements ClusterService {
    private static final String CLUSTERING_URL = "http://localhost:8000/cluster";
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Override
    public void cluster() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLUSTERING_URL))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Clustering completed successfully. Response: {}", response.body());
            } else {
                log.error("Clustering failed with status code: {} and body: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error during clustering: {}", e.getMessage(), e);
        }
    }

}


