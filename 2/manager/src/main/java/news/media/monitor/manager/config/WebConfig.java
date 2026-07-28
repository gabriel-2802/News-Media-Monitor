package news.media.monitor.manager.config;

import news.media.monitor.manager.security.SystemTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SystemTokenProvider systemTokenProvider;

    @Bean
    RestTemplate getRestTemplate() {
        var restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + systemTokenProvider.getToken());
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
