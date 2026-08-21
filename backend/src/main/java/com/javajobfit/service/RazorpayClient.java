package com.javajobfit.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The only place that talks to Razorpay over HTTP. Kept behind one narrow method so the payment
 * flow can be tested without a network or an account.
 */
@Component
public class RazorpayClient {
    private static final String PAYMENT_LINKS_URL = "https://api.razorpay.com/v1/payment_links";

    private final RestTemplate restTemplate;
    private final String keyId;
    private final String keySecret;

    public RazorpayClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.payment.razorpay-key-id:}") String keyId,
            @Value("${app.payment.razorpay-key-secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(20))
                .build();
    }

    /**
     * Creates a hosted payment link. {@code referenceId} is the report's public id, which is what
     * the webhook uses to find the report again — Razorpay requires it to be unique per link.
     */
    public PaymentLink createPaymentLink(String referenceId, int amountPaise, String currency, String returnUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amountPaise);
        body.put("currency", currency);
        body.put("accept_partial", false);
        body.put("reference_id", referenceId);
        body.put("description", "JavaJobFit Pro Report");
        body.put("callback_url", returnUrl);
        body.put("callback_method", "get");
        body.put("reminder_enable", false);
        Map<String, Object> notify = new LinkedHashMap<>();
        // We collect no contact details for the payment, so let Razorpay's own page handle it.
        notify.put("sms", false);
        notify.put("email", false);
        body.put("notify", notify);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                PAYMENT_LINKS_URL, new HttpEntity<>(body, headers), Map.class);
        if (response == null || response.get("short_url") == null || response.get("id") == null) {
            throw new IllegalStateException("Razorpay did not return a usable payment link");
        }
        return new PaymentLink(String.valueOf(response.get("id")), String.valueOf(response.get("short_url")));
    }

    /** Minimal view of a created payment link. */
    public static final class PaymentLink {
        private final String id;
        private final String url;

        public PaymentLink(String id, String url) {
            this.id = id;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }
    }
}
