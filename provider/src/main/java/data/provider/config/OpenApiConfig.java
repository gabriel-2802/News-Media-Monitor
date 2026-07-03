package data.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI providerOpenAPI() {
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
                                .email("gabrielvalentine738@gmail.com")));
    }
}
