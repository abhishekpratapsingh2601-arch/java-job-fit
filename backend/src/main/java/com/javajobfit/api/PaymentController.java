package com.javajobfit.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javajobfit.api.dto.PaymentLinkRequest;
import com.javajobfit.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Lets the results page show the real price and hide the CTA when payments are off. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", paymentService.isProviderEnabled());
        body.put("priceInr", paymentService.getPriceInr());
        body.put("currency", "INR");
        return body;
    }

    @PostMapping("/link")
    public ResponseEntity<Map<String, String>> createLink(@Valid @RequestBody PaymentLinkRequest request) {
        if (!paymentService.isProviderEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Collections.singletonMap("error", "Payments are not enabled yet."));
        }
        String url = paymentService.createPaymentLink(request.getPublicId());
        return ResponseEntity.ok(Collections.singletonMap("paymentUrl", url));
    }

    /**
     * Razorpay webhook. Takes the body as a raw String because the signature is computed over the
     * exact bytes sent — binding to a DTO and re-serializing would change the whitespace and break
     * verification.
     *
     * <p>Always answers 200 once the signature is valid, including for events we ignore and for
     * replays: providers retry on non-2xx, and retrying an event we deliberately skipped is noise.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        boolean unlocked = paymentService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok(Collections.singletonMap("unlocked", unlocked));
    }
}
