package com.bank.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Centralised audit writer. Call from service code on any state-changing action.
 * Writes are REQUIRES_NEW so an audit row persists even if the business transaction
 * rolls back — required for security incident forensics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnClass(jakarta.persistence.EntityManager.class)
public class AuditLogger {

    private final AuditEventRepository repository;
    private final ObjectMapper mapper;
    private final HttpServletRequest currentRequest;

    @Value("${spring.application.name:unknown-service}") String serviceName;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String resourceType, String resourceId, Map<String, Object> details) {
        long seq = repository.maxSequence().orElse(0L) + 1;
        AuditEvent prev = repository.findTopByOrderBySequenceDesc();
        String prevHash = prev == null ? "GENESIS" : prev.getRowHash();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? null : String.valueOf(auth.getName());
        String roles = auth == null ? "" : auth.getAuthorities().toString();
        String requestId = currentRequest == null ? null : currentRequest.getHeader("X-Request-Id");

        String detailsJson;
        try {
            detailsJson = mapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException e) {
            detailsJson = "{}";
        }

        AuditEvent ev = AuditEvent.builder()
                .id(UUID.randomUUID())
                .sequence(seq)
                .at(Instant.now())
                .actorId(actor)
                .actorRoles(roles)
                .requestId(requestId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(detailsJson)
                .prevHash(prevHash)
                .serviceName(serviceName)
                .build();
        ev.setRowHash(hashOf(ev));
        repository.save(ev);
    }

    private static String hashOf(AuditEvent e) {
        // Canonical form: deterministic, omits the hash itself.
        String payload = String.join("|",
                e.getSequence().toString(),
                e.getAt().toString(),
                nz(e.getActorId()),
                nz(e.getActorRoles()),
                nz(e.getRequestId()),
                nz(e.getAction()),
                nz(e.getResourceType()),
                nz(e.getResourceId()),
                nz(e.getDetails()),
                nz(e.getPrevHash()),
                nz(e.getServiceName()));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
