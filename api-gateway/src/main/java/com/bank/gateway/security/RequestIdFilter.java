package com.bank.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Generates X-Request-Id if absent and propagates it downstream + back in response.
 * Cheap, but it makes audit logs and incident response usable.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        final String requestId = id;
        var req = exchange.getRequest().mutate().header("X-Request-Id", requestId).build();
        exchange.getResponse().getHeaders().set("X-Request-Id", requestId);
        return chain.filter(exchange.mutate().request(req).build());
    }

    @Override
    public int getOrder() { return -200; }
}
