package com.javajobfit.service;

import java.security.SecureRandom;
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
    /** A UUID is 36 chars; Razorpay allows 40 for reference_id, leaving room for "-" + 3 chars. */
    private static final int UUID_LENGTH = 36;
    private static final String SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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
     * the existing one rather than creating a duplicate, so a user who clicks twice does not get two
     * invoices — but only while that link is still for the current price.
     *
     * <p>Not {@code @Transactional}: the Razorpay call can take up to 30s and must not hold a pooled
     * DB connection open. Each {@code reference_id} gets a short random suffix because Razorpay
     * rejects a repeated one; without it, a link Razorpay created whose response we never saved
     * (timeout, failed insert) would make the report permanently unbuyable.
     */
    public String createPaymentLink(String reportPublicId) {
        Report report = reportRepository.findByPublicId(parseUuid(reportPublicId))
                .orElseThrow(() -> new ReportNotFoundException(reportPublicId));
        if (report.isPaid()) {
            throw new AlreadyPaidException(reportPublicId);
        }

        Optional<Payment> existing = paymentRepository
                .findFirstByReportPublicIdAndStatusOrderByIdDesc(report.getPublicId(), Payment.STATUS_CREATED);
        if (existing.isPresent() && existing.get().getPaymentLinkUrl() != null
                && existing.get().getAmountPaise() == priceInPaise()) {
            return existing.get().getPaymentLinkUrl();
        }

        RazorpayClient.PaymentLink link = razorpayClient.createPaymentLink(
                referenceIdFor(report.getPublicId()),
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
            // Signed by Razorpay but unreadable: acknowledge it, or Razorpay retries it forever.
            log.warn("Ignoring a signed payment webhook with an unparseable body");
            return false;
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
        String currency = linkEntity.path("currency").asText(CURRENCY_INR);
        String providerPaymentId = root.path("payload").path("payment").path("entity").path("id").asText("");

        // Every real payment_link.paid event names its link. Without an id there is nothing to
        // de-duplicate on, so each replay would insert another audit row.
        if (linkId.isEmpty()) {
            log.warn("Ignoring a payment webhook with no payment link id");
            return false;
        }

        // Judge the payment against the price of the link that was paid, not today's price: a buyer
        // who opened an ₹89 link before a price change has still paid in full.
        Optional<Payment> knownLink = paymentRepository.findByPaymentLinkId(linkId);
        int expectedPaise = knownLink.map(Payment::getAmountPaise).orElse(priceInPaise());
        if (!CURRENCY_INR.equalsIgnoreCase(currency)) {
            log.warn("Refusing to unlock report {}: paid in {}, expected {}", referenceId, currency, CURRENCY_INR);
            return false;
        }
        if (amountPaid < expectedPaise) {
            log.warn("Refusing to unlock report {}: paid {} paise, expected {}",
                    referenceId, amountPaid, expectedPaise);
            return false;
        }

        Optional<Report> maybeReport = knownLink.isPresent()
                ? reportRepository.findByPublicId(knownLink.get().getReportPublicId())
                : findReportByReference(referenceId);
        if (maybeReport.isEmpty()) {
            log.warn("Payment webhook referenced unknown report {}", referenceId);
            return false;
        }

        Payment payment = knownLink.orElseGet(() -> {
            // A link created outside this app (or before a redeploy) still deserves an audit row.
            Payment created = new Payment();
            created.setReportPublicId(maybeReport.get().getPublicId());
            created.setProvider(PROVIDER_RAZORPAY);
            created.setPaymentLinkId(linkId);
            created.setAmountPaise(amountPaid);
            created.setCurrency(CURRENCY_INR);
            created.setStatus(Payment.STATUS_CREATED);
            return created;
        });

        Report report = maybeReport.get();
        if (Payment.STATUS_PAID.equals(payment.getStatus()) && report.isPaid()) {
            log.info("Payment webhook replay for already-unlocked report {}", referenceId);
            return false;
        }

        boolean alreadyUnlocked = report.isPaid();
        // Both markPaid calls are idempotent, so a late or repeated event never rewrites the
        // original payment time or id.
        report.markPaid();
        payment.markPaid(providerPaymentId.isEmpty() ? null : providerPaymentId);
        reportRepository.save(report);
        paymentRepository.save(payment);
        if (alreadyUnlocked) {
            // A second link for the same report was also paid: the money was really taken, so it
            // is recorded, and the refund policy promises to refund duplicate charges.
            log.warn("Duplicate payment on link {} for already-unlocked report {}: refund it", linkId, referenceId);
            return false;
        }
        log.info("Unlocked report {} after confirmed payment", referenceId);
        return true;
    }

    /** The report's public id plus "-" and three random chars: unique per link, 40 chars max. */
    static String referenceIdFor(UUID publicId) {
        StringBuilder reference = new StringBuilder(publicId.toString()).append('-');
        for (int i = 0; i < 3; i++) {
            reference.append(SUFFIX_ALPHABET.charAt(RANDOM.nextInt(SUFFIX_ALPHABET.length())));
        }
        return reference.toString();
    }

    /** Accepts both the plain public id (links made before suffixes) and the suffixed form. */
    private Optional<Report> findReportByReference(String referenceId) {
        if (referenceId == null || referenceId.length() < UUID_LENGTH) {
            return Optional.empty();
        }
        try {
            return reportRepository.findByPublicId(UUID.fromString(referenceId.substring(0, UUID_LENGTH)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
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
