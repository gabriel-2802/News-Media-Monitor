package data.provider.config;

import data.provider.security.JwtAccessDeniedHandler;
import data.provider.security.JwtAuthenticationEntryPoint;
import data.provider.security.JwtAuthenticationFilter;
import data.provider.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Reads are open to everyone; only mutating endpoints require a role.
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()

                        .requestMatchers(HttpMethod.POST, Constants.ARTICLES_BASE_PATH + Constants.ARTICLES_TRIGGER_SCRAPE_PATH)
                        .hasRole(Constants.ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, Constants.ARTICLES_BASE_PATH)
                        .hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SYSTEM)
                        .requestMatchers(HttpMethod.PATCH, Constants.ARTICLES_BASE_PATH + Constants.ARTICLES_SET_TOPIC_PATH)
                        .hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SYSTEM)

                        .requestMatchers(HttpMethod.PATCH, Constants.NEWS_SOURCES_BASE_PATH + Constants.NEWS_SOURCE_RESET_PATH)
                        .hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SYSTEM)
                        .requestMatchers(HttpMethod.PATCH, Constants.NEWS_SOURCES_BASE_PATH + Constants.NEWS_SOURCE_FAILURE_PATH)
                        .hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SYSTEM)
                        .requestMatchers(HttpMethod.POST, Constants.NEWS_SOURCES_BASE_PATH)
                        .hasRole(Constants.ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, Constants.NEWS_SOURCES_BASE_PATH + Constants.NEWS_SOURCE_BY_NAME_PATH)
                        .hasRole(Constants.ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, Constants.STORIES_BASE_PATH)
                        .hasRole(Constants.ROLE_SYSTEM)
                        .requestMatchers(HttpMethod.PATCH, Constants.STORIES_BASE_PATH + Constants.STORIES_ATTACH_PATH)
                        .hasRole(Constants.ROLE_SYSTEM)

                        .requestMatchers(Constants.SUBSCRIPTIONS_BASE_PATH + "/**")
                        .hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SYSTEM)

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
