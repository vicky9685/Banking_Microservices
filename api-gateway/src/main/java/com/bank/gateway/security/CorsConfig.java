package com.bank.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;

/**
 * CORS is the edge-layer allow-list for browser clients. Allowed origins come
 * from configuration so production can pin to specific domains and dev can be
 * looser via override.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        Arrays.stream(origins.split(",")).map(String::trim).forEach(cfg::addAllowedOrigin);
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id"));
        cfg.setExposedHeaders(Arrays.asList("X-Request-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(Duration.ofHours(1));

        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(src);
    }
}
