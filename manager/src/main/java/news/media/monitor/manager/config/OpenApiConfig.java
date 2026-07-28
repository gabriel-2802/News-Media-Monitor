package news.media.monitor.manager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import news.media.monitor.manager.controllers.AccountController;
import news.media.monitor.manager.controllers.AdminController;
import news.media.monitor.manager.controllers.AuthController;
import news.media.monitor.manager.controllers.NotificationController;
import news.media.monitor.manager.controllers.SubscriptionController;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME   = "bearerAuth";
    private static final String BEARER_SCHEME          = "bearer";
    private static final String BEARER_FORMAT          = "JWT";
    private static final String API_TITLE              = "News Media Monitor Manager";
    private static final String API_DESCRIPTION        = "Authentication and user management";
    private static final String API_VERSION            = "1.0";

    // Swagger UI groups/orders operations by this list when present, instead of sorting tags alphabetically.
    private static final List<Tag> TAGS_IN_DECLARATION_ORDER = List.of(
            new Tag().name("Auth").description("Endpoints for user registration and authentication"),
            new Tag().name("Account").description("Endpoints for the currently authenticated user's own account"),
            new Tag().name("Notifications").description("Endpoints for the currently authenticated user's own notifications"),
            new Tag().name("Subscriptions").description("Endpoints for the currently authenticated user's own topic and story subscriptions"),
            new Tag().name("Admin").description("Endpoints for administrators to manage users, roles, and account state")
    );

    // Controllers in the order their endpoints should appear within the docs; method order inside each is
    // taken from source declaration order via reflection.
    private static final List<Class<?>> CONTROLLERS_IN_ORDER = List.of(
            AuthController.class,
            AccountController.class,
            NotificationController.class,
            SubscriptionController.class,
            AdminController.class
    );

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .description(API_DESCRIPTION)
                        .version(API_VERSION))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme(BEARER_SCHEME)
                                        .bearerFormat(BEARER_FORMAT)))
                .tags(TAGS_IN_DECLARATION_ORDER);
    }

    @Bean
    public GlobalOpenApiCustomizer operationOrderCustomizer() {
        List<String> operationIdOrder = operationIdOrder();
        return openApi -> reorderPaths(openApi, operationIdOrder);
    }

    private void reorderPaths(OpenAPI openApi, List<String> operationIdOrder) {
        Paths originalPaths = openApi.getPaths();
        if (originalPaths == null) {
            return;
        }

        record Entry(String path, PathItem.HttpMethod method, Operation operation) {}

        List<Entry> entries = new ArrayList<>();
        originalPaths.forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((httpMethod, operation) ->
                        entries.add(new Entry(path, httpMethod, operation))));

        entries.sort((a, b) -> Integer.compare(
                indexOf(a.operation(), operationIdOrder),
                indexOf(b.operation(), operationIdOrder)));

        Paths orderedPaths = new Paths();
        for (Entry entry : entries) {
            PathItem pathItem = orderedPaths.computeIfAbsent(entry.path(), p -> new PathItem());
            pathItem.operation(entry.method(), entry.operation());
        }
        openApi.setPaths(orderedPaths);
    }

    private int indexOf(Operation operation, List<String> order) {
        int idx = order.indexOf(operation.getOperationId());
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }

    private List<String> operationIdOrder() {
        List<String> order = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS_IN_ORDER) {
            for (Method method : controller.getDeclaredMethods()) {
                if (hasMappingAnnotation(method)) {
                    order.add(method.getName());
                }
            }
        }
        return order;
    }

    private boolean hasMappingAnnotation(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == RequestMapping.class || type.isAnnotationPresent(RequestMapping.class)) {
                return true;
            }
        }
        return false;
    }
}