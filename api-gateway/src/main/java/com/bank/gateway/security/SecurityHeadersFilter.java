package com.bank.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * OWASP-recommended security headers added to every response.
 * Runs last so it cannot be stripped by downstream filters.
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders h = exchange.getResponse().getHeaders();
            h.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            h.set("X-Content-Type-Options", "nosniff");
            h.set("X-Frame-Options", "DENY");
            h.set("Referrer-Policy", "no-referrer");
            h.set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            h.set("Content-Security-Policy",
                    "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; " +
                    "script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'");
            h.set("Cross-Origin-Opener-Policy", "same-origin");
            h.set("Cross-Origin-Resource-Policy", "same-site");
        }));
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
