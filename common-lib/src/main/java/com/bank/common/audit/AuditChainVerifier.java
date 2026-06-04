package com.bank.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Periodically walks the audit chain checking each row's prev/row hash.
 * Any break is logged at ERROR for the SIEM to pick up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditChainVerifier {

    private final AuditEventRepository repository;

    @Scheduled(cron = "${app.audit.verify-cron:0 */15 * * * *}")
    public void verify() {
        List<AuditEvent> all = repository.findAll(org.springframework.data.domain.Sort.by("sequence"));
        String expectedPrev = "GENESIS";
        for (AuditEvent ev : all) {
            if (!expectedPrev.equals(ev.getPrevHash())) {
                log.error("Audit chain break at seq={}: prevHash mismatch", ev.getSequence());
                return;
            }
            String recomputed = computeHash(ev);
            if (!recomputed.equals(ev.getRowHash())) {
                log.error("Audit chain tamper at seq={}: rowHash mismatch", ev.getSequence());
                return;
            }
            expectedPrev = ev.getRowHash();
        }
        log.debug("Audit chain verified up to {} rows", all.size());
    }

    private static String computeHash(AuditEvent e) {
        String payload = String.join("|",
                e.getSequence().toString(),
                e.getAt().toString(),
                ns(e.getActorId()),
                ns(e.getActorRoles()),
                ns(e.getRequestId()),
                ns(e.getAction()),
                ns(e.getResourceType()),
                ns(e.getResourceId()),
                ns(e.getDetails()),
                ns(e.getPrevHash()),
                ns(e.getServiceName()));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String ns(String s) { return s == null ? "" : s; }
}
