package com.bank.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Internal services reject any request that didn't go through the gateway.
 * The gateway is the only place that knows the shared HMAC key and stamps
 * X-Service-Signature on every proxied request.
 *
 * Disabled with app.security.service-auth.enabled=false for local non-K8s runs
 * where you want to curl backends directly.
 */
@Slf4j
public class ServiceSignatureVerificationFilter implements Filter {

    private final String sharedKey;

    public ServiceSignatureVerificationFilter(
            @Value("${app.security.service-auth.hmac-key:${service.hmac.key:dev-only-internal-shared-key-please-rotate}}") String sharedKey) {
        this.sharedKey = sharedKey;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest http) || !(res instanceof HttpServletResponse out)) {
            chain.doFilter(req, res);
            return;
        }
        String path = http.getRequestURI();
        // Allow health and Camunda webapp endpoints (they have their own auth)
        if (path.startsWith("/actuator") || path.startsWith("/camunda") || path.startsWith("/engine-rest")) {
            chain.doFilter(req, res);
            return;
        }
        boolean ok = ServiceSignature.verify(
                sharedKey,
                http.getMethod(),
                path,
                http.getHeader(ServiceSignature.H_TIMESTAMP),
                http.getHeader(ServiceSignature.H_REQUEST_ID),
                http.getHeader(ServiceSignature.H_USER_ID),
                http.getHeader(ServiceSignature.H_SIGNATURE)
        );
        if (!ok) {
            log.warn("Rejected unsigned request to {} {}", http.getMethod(), path);
            out.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.getWriter().write("{\"success\":false,\"errorCode\":\"UNSIGNED_REQUEST\",\"message\":\"Direct backend access denied\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
