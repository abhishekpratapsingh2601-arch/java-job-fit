package com.javajobfit.service;

import static org.assertj.core.api.Assertions.assertThat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {
    private static final String SECRET = "whsec_test_9f2a";
    private static final String BODY = "{\"event\":\"payment_link.paid\",\"payload\":{}}";

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    void acceptsASignatureProducedWithTheSharedSecret() throws Exception {
        assertThat(verifier.isValid(BODY, sign(BODY, SECRET), SECRET)).isTrue();
    }

    @Test
    void rejectsASignatureFromTheWrongSecret() throws Exception {
        assertThat(verifier.isValid(BODY, sign(BODY, "whsec_attacker"), SECRET)).isFalse();
    }

    @Test
    void rejectsWhenTheBodyWasTamperedWithAfterSigning() throws Exception {
        String signature = sign(BODY, SECRET);
        String tampered = BODY.replace("payment_link.paid", "payment_link.PAID");

        assertThat(verifier.isValid(tampered, signature, SECRET)).isFalse();
    }

    @Test
    void rejectsMissingOrEmptyInputsRatherThanTreatingThemAsValid() throws Exception {
        assertThat(verifier.isValid(BODY, null, SECRET)).isFalse();
        assertThat(verifier.isValid(null, sign(BODY, SECRET), SECRET)).isFalse();
        assertThat(verifier.isValid(BODY, "", SECRET)).isFalse();
        assertThat(verifier.isValid(BODY, sign(BODY, SECRET), "")).isFalse();
        assertThat(verifier.isValid(BODY, sign(BODY, SECRET), null)).isFalse();
    }

    @Test
    void toleratesSurroundingWhitespaceInTheHeaderValue() throws Exception {
        assertThat(verifier.isValid(BODY, "  " + sign(BODY, SECRET) + "  ", SECRET)).isTrue();
    }
}
