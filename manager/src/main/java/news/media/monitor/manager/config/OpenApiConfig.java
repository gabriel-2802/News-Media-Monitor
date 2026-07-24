package news.media.monitor.manager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME   = "bearerAuth";
    private static final String BEARER_SCHEME          = "bearer";
    private static final String BEARER_FORMAT          = "JWT";
    private static final String API_TITLE              = "CLM User Service API";
    private static final String API_DESCRIPTION        = "Authentication and user management";
    private static final String API_VERSION            = "1.0";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url("https://localhost")
                        .description("News Media Monitor Manager - API"))
                .info(new Info()
                        .title(API_TITLE)
                        .description(API_DESCRIPTION)
                        .version(API_VERSION))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme(BEARER_SCHEME)
                                        .bearerFormat(BEARER_FORMAT)));
    }
}