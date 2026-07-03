package data.provider.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class Util {

    private static final long PING_TIMEOUT_SECONDS = 5;
    private static final String PING_HTTP_METHOD = "HEAD";
    private static final int PING_SUCCESS_STATUS_MIN = 200;
    private static final int PING_SUCCESS_STATUS_MAX = 400;
    private static final String URL_PING_FAILED_LOG = "Failed to reach URL: {}";
    private static final String INTERRUPTED_EXCEPTION_LOG = "Thread was interrupted while trying to reach URL: {}";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private Util() {}

    public static boolean isNotReachable(final String url) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method(PING_HTTP_METHOD, HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(PING_TIMEOUT_SECONDS))
                    .build();

            final HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < PING_SUCCESS_STATUS_MIN
                    || response.statusCode() >= PING_SUCCESS_STATUS_MAX;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(INTERRUPTED_EXCEPTION_LOG, e.getMessage());
            return true;
        } catch (Exception e) {
            log.warn(URL_PING_FAILED_LOG, url, e);
            return true;
        }
    }
}

