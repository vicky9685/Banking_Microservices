package com.bank.gateway.security;

import com.bank.common.security.ServiceSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Stamps every proxied request with the internal HMAC signature so backends can
 * verify the call went through the gateway and isn't a direct hit.
 *
 * Must run AFTER JwtAuthenticationFilter so X-User-Id is present in the payload.
 */
@Component
public class ServiceSignatureSigningFilter implements GlobalFilter, Ordered {

    private final String sharedKey;

    public ServiceSignatureSigningFilter(
            @Value("${app.security.service-auth.hmac-key:${service.hmac.key:dev-only-internal-shared-key-please-rotate}}") String sharedKey) {
        this.sharedKey = sharedKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String requestId = exchange.getRequest().getHeaders().getFirst(ServiceSignature.H_REQUEST_ID);
        String userId = exchange.getRequest().getHeaders().getFirst(ServiceSignature.H_USER_ID);

        String sig = ServiceSignature.sign(sharedKey, method, path, ts, requestId, userId);
        var req = exchange.getRequest().mutate()
                .header(ServiceSignature.H_TIMESTAMP, ts)
                .header(ServiceSignature.H_SIGNATURE, sig)
                .build();
        return chain.filter(exchange.mutate().request(req).build());
    }

    @Override
    public int getOrder() { return -50; }   // after JWT filter (-100), before signature checks downstream
}
