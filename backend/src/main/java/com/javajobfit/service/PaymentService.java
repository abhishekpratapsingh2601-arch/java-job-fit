package com.javajobfit.service;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javajobfit.domain.Payment;
import com.javajobfit.domain.Report;
import com.javajobfit.repository.PaymentRepository;
import com.javajobfit.repository.ReportRepository;

/**
 * Turns a free report into a paid one, and nothing else can.
 *
 * <p>The only path to {@code paid = true} is {@link #handleWebhook}, which requires a valid
 * Razorpay signature over the raw body <em>and</em> an amount matching the configured price. The
 * browser is never trusted, and the create-link endpoint deliberately grants nothing.
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String PROVIDER_RAZORPAY = "razorpay";
    private static final String CURRENCY_INR = "INR";
    private static final String EVENT_PAYMENT_LINK_PAID = "payment_link.paid";

    private final ReportRepository reportRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final boolean providerEnabled;
    private final int priceInr;
    private final String webhookSecret;
    private final String returnUrl;

    public PaymentService(
            ReportRepository reportRepository,
            PaymentRepository paymentRepository,
            RazorpayClient razorpayClient,
            WebhookSignatureVerifier signatureVerifier,
            @Value("${app.payment.provider-enabled:false}") boolean providerEnabled,
            @Value("${app.payment.price-inr:89}") int priceInr,
            @Value("${app.payment.razorpay-webhook-secret:}") String webhookSecret,
            @Value("${app.payment.return-url:}") String returnUrl) {
        this.reportRepository = reportRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayClient = razorpayClient;
        this.signatureVerifier = signatureVerifier;
        this.providerEnabled = providerEnabled;
        this.priceInr = priceInr;
        this.webhookSecret = webhookSecret;
        this.returnUrl = returnUrl;
    }

    public boolean isProviderEnabled() {
        return providerEnabled;
    }

    public int getPriceInr() {
        return priceInr;
    }

    private int priceInPaise() {
        return priceInr * 100;
    }

    /**
     * Returns a hosted checkout URL for a report. Re-requesting a link for the same report returns
     * the existing one rather than creating a duplicate — Razorpay rejects a repeated
     * {@code reference_id}, and a user who clicks twice should not generate two invoices.
     */
    @Transactional
    public String createPaymentLink(String reportPublicId) {
        Report report = reportRepository.findByPublicId(parseUuid(reportPublicId))
                .orElseThrow(() -> new ReportNotFoundException(reportPublicId));
        if (report.isPaid()) {
            throw new AlreadyPaidException(reportPublicId);
        }

        Optional<Payment> existing = paymentRepository
                .findFirstByReportPublicIdAndStatusOrderByIdDesc(report.getPublicId(), Payment.STATUS_CREATED);
        if (existing.isPresent() && existing.get().getPaymentLinkUrl() != null) {
            return existing.get().getPaymentLinkUrl();
        }

        RazorpayClient.PaymentLink link = razorpayClient.createPaymentLink(
                report.getPublicId().toString(),
                priceInPaise(),
                CURRENCY_INR,
                buildReturnUrl(report.getPublicId().toString()));

        Payment payment = new Payment();
        payment.setReportPublicId(report.getPublicId());
        payment.setProvider(PROVIDER_RAZORPAY);
        payment.setPaymentLinkId(link.getId());
        payment.setPaymentLinkUrl(link.getUrl());
        payment.setAmountPaise(priceInPaise());
        payment.setCurrency(CURRENCY_INR);
        payment.setStatus(Payment.STATUS_CREATED);
        paymentRepository.save(payment);

        return link.getUrl();
    }

    /**
     * Processes a provider webhook. Returns true when a report was unlocked.
     *
     * <p>Rejects anything without a valid signature, ignores events other than a paid link, and
     * refuses to unlock when the amount paid does not match the configured price — otherwise a
     * hand-crafted link for &#8377;1 would buy a &#8377;{@code priceInr} report. Safe to call
     * repeatedly: providers retry until they get a 2xx, so a replay must be a no-op, not a
     * second unlock.
     */
    @Transactional
    public boolean handleWebhook(String rawBody, String signature) {
        if (!signatureVerifier.isValid(rawBody, signature, webhookSecret)) {
            log.warn("Rejected payment webhook with an invalid signature");
            throw new InvalidWebhookSignatureException();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Rejected payment webhook with an unparseable body");
            throw new InvalidWebhookSignatureException();
        }

        String event = root.path("event").asText("");
        if (!EVENT_PAYMENT_LINK_PAID.equals(event)) {
            log.info("Ignoring payment webhook event {}", event);
            return false;
        }

        JsonNode linkEntity = root.path("payload").path("payment_link").path("entity");
        String linkId = linkEntity.path("id").asText("");
        String referenceId = linkEntity.path("reference_id").asText("");
        int amountPaid = linkEntity.path("amount_paid").asInt(linkEntity.path("amount").asInt(0));
        String providerPaymentId = root.path("payload").path("payment").path("entity").path("id").asText("");

        if (amountPaid < priceInPaise()) {
            log.warn("Refusing to unlock report {}: paid {} paise, expected {}",
                    referenceId, amountPaid, priceInPaise());
            return false;
        }

        Optional<Report> maybeReport = reportRepository.findByPublicId(parseUuid(referenceId));
        if (maybeReport.isEmpty()) {
            log.warn("Payment webhook referenced unknown report {}", referenceId);
            return false;
        }

        Payment payment = paymentRepository.findByPaymentLinkId(linkId).orElseGet(() -> {
            // A link created outside this app (or before a redeploy) still deserves an audit row.
            Payment created = new Payment();
            created.setReportPublicId(maybeReport.get().getPublicId());
            created.setProvider(PROVIDER_RAZORPAY);
            created.setPaymentLinkId(linkId.isEmpty() ? null : linkId);
            created.setAmountPaise(amountPaid);
            created.setCurrency(CURRENCY_INR);
            created.setStatus(Payment.STATUS_CREATED);
            return created;
        });

        Report report = maybeReport.get();
        if (report.isPaid() && Payment.STATUS_PAID.equals(payment.getStatus())) {
            log.info("Payment webhook replay for already-unlocked report {}", referenceId);
            return false;
        }

        report.markPaid();
        payment.markPaid(providerPaymentId.isEmpty() ? null : providerPaymentId);
        reportRepository.save(report);
        paymentRepository.save(payment);
        log.info("Unlocked report {} after confirmed payment", referenceId);
        return true;
    }

    private String buildReturnUrl(String publicId) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return "";
        }
        String separator = returnUrl.contains("?") ? "&" : "?";
        return returnUrl + separator + "report=" + publicId + "&paid=1";
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new ReportNotFoundException(String.valueOf(value));
        }
    }

    /** Thrown when a webhook cannot be proven to come from the provider. */
    public static class InvalidWebhookSignatureException extends RuntimeException {
        public InvalidWebhookSignatureException() {
            super("Invalid webhook signature");
        }
    }

    /** Thrown when a checkout is requested for a report that is already unlocked. */
    public static class AlreadyPaidException extends RuntimeException {
        public AlreadyPaidException(String reportPublicId) {
            super("Report already paid: " + reportPublicId);
        }
    }
}
