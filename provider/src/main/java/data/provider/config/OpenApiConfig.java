package data.provider.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String BEARER_SCHEME = "bearer";
    private static final String BEARER_FORMAT = "JWT";

    @Bean
    OpenAPI providerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("News Provider API")
                        .description("""
                                Ingestion and read API for the news monitoring pipeline. Exposes endpoints \
                                for registering news sources and persisting/retrieving scraped articles \
                                backed by Neo4j.""")
                        .version("v1")
                        .contact(new Contact()
                                .name("Gabriel Cărăuleanu")
                                .email("gabrielvalentine738@gmail.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme(BEARER_SCHEME)
                                        .bearerFormat(BEARER_FORMAT)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
