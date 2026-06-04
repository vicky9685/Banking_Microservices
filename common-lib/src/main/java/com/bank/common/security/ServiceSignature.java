package com.bank.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * HMAC-SHA256 over METHOD|PATH|TIMESTAMP|REQUEST_ID|USER_ID using a shared internal key
 * (sourced from Vault, key id: secret/banking/common, key: service.hmac.key).
 *
 * Why HMAC and not mTLS?
 *   - mTLS is the right answer at the mesh layer (Istio/Linkerd) and is recommended.
 *   - HMAC is a defence-in-depth layer that also works in any topology (compose, kind,
 *     Cloud Run without a mesh), and prevents accidental direct-to-backend calls
 *     even when network policies don't apply.
 *
 * Anti-replay: requests with timestamps older than {@link #MAX_SKEW} are rejected.
 */
public final class ServiceSignature {

    public static final String H_SIGNATURE  = "X-Service-Signature";
    public static final String H_TIMESTAMP  = "X-Service-Timestamp";
    public static final String H_REQUEST_ID = "X-Request-Id";
    public static final String H_USER_ID    = "X-User-Id";

    public static final Duration MAX_SKEW = Duration.ofMinutes(5);

    private ServiceSignature() {}

    public static String sign(String key, String method, String path, String timestamp,
                              String requestId, String userId) {
        try {
            String payload = String.join("|",
                    method.toUpperCase(),
                    path,
                    timestamp,
                    nullSafe(requestId),
                    nullSafe(userId));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    public static boolean verify(String key, String method, String path, String timestamp,
                                 String requestId, String userId, String signature) {
        if (signature == null || timestamp == null) return false;
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - ts) > MAX_SKEW.getSeconds()) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        String expected = sign(key, method, path, timestamp, requestId, userId);
        return constantTimeEquals(expected, signature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
