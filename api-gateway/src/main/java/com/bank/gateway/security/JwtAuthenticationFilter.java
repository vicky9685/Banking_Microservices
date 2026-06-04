package com.bank.gateway.security;

import com.bank.common.security.ServiceSignature;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Edge JWT verification. Validates signature, injects user/role headers downstream.
 * Acts as the perimeter for the zero-trust internal network — downstream services
 * trust X-User-Id and X-User-Roles set here.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Set<String> OPEN_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator/health",
            "/fallback",
            "/camunda",        // Camunda webapps use their own basic auth
            "/engine-rest"     // Camunda REST has its own auth filter
    );

    private final SecretKey key;
    private final String issuer;

    public JwtAuthenticationFilter(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer:banking-platform}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Strip sensitive internal headers from the client's request to prevent header spoofing/injection
        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Roles");
                    headers.remove("X-Customer-Id");
                    headers.remove(ServiceSignature.H_SIGNATURE);
                    headers.remove(ServiceSignature.H_TIMESTAMP);
                });

        String path = request.getPath().value();

        if (OPEN_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "missing-token");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<?> roles = claims.get("roles", List.class);
            String rolesHeader = roles == null ? "" : String.join(",", roles.stream().map(Object::toString).toList());
            String customerId = claims.get("customerId", String.class);

            requestBuilder
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Roles", rolesHeader);

            if (customerId != null && !customerId.isBlank()) {
                requestBuilder.header("X-Customer-Id", customerId);
            }

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return unauthorized(exchange, "invalid-token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", reason);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
