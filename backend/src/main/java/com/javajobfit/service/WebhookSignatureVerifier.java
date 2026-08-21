package com.javajobfit.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Verifies Razorpay's {@code X-Razorpay-Signature} header: HMAC-SHA256 of the exact raw request
 * body, keyed with the webhook secret.
 *
 * <p>This is the only thing standing between a stranger's HTTP request and a free paid report, so
 * it verifies against the raw bytes (never a re-serialized DTO, whose whitespace would differ) and
 * compares in constant time.
 */
@Component
public class WebhookSignatureVerifier {
    private static final String ALGORITHM = "HmacSHA256";

    public boolean isValid(String rawBody, String providedSignature, String secret) {
        if (rawBody == null || providedSignature == null || secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder expected = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                expected.append(Character.forDigit((b >> 4) & 0xF, 16));
                expected.append(Character.forDigit(b & 0xF, 16));
            }
            return MessageDigest.isEqual(
                    expected.toString().getBytes(StandardCharsets.UTF_8),
                    providedSignature.trim().getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            return false;
        }
    }
}
