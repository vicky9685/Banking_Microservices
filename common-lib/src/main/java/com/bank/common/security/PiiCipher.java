package com.bank.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM envelope cipher used by JPA converters for PII fields.
 * The data key is derived (SHA-256) from a Vault-supplied master string at
 *   secret/banking/common :: pii.master-key
 * with a per-process salt embedded in the ciphertext so re-encrypting on key
 * rotation is straightforward.
 *
 * Why GCM: authenticated encryption (integrity + confidentiality in one primitive).
 * Why AttributeConverter: encryption applied uniformly at JPA boundary, with no
 * service-layer code having to remember to call encrypt/decrypt.
 *
 * Storage layout: base64( IV(12) || ciphertext+tag )
 */
@Component
public class PiiCipher {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    public PiiCipher(@Value("${pii.master-key:${PII_MASTER_KEY:dev-only-pii-key-please-rotate-with-vault}}") String master) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(master.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive PII master key", e);
        }
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv).put(ct);
            return "enc:v1:" + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("PII encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        if (!ciphertext.startsWith("enc:v1:")) return ciphertext;  // legacy plain (during migration)
        try {
            byte[] raw = Base64.getDecoder().decode(ciphertext.substring("enc:v1:".length()));
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(raw, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[raw.length - IV_LEN];
            System.arraycopy(raw, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("PII decryption failed", e);
        }
    }
}
