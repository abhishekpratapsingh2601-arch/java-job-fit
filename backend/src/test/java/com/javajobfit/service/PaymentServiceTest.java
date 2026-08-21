package com.javajobfit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.javajobfit.api.dto.ReportRequest;
import com.javajobfit.api.dto.ReportResponse;
import com.javajobfit.domain.Payment;
import com.javajobfit.domain.Report;
import com.javajobfit.repository.PaymentRepository;
import com.javajobfit.repository.ReportRepository;

/**
 * The money path. Every test here is really asking one question: can a stranger get a paid report
 * without paying?
 */
@SpringBootTest
class PaymentServiceTest {
    private static final String SECRET = "whsec_test_9f2a";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private RazorpayClient razorpayClient;

    private String publicId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        // Enable the provider and pin the secret/price for these tests only.
        ReflectionTestUtils.setField(paymentService, "providerEnabled", true);
        ReflectionTestUtils.setField(paymentService, "webhookSecret", SECRET);
        ReflectionTestUtils.setField(paymentService, "priceInr", 89);

        ReportRequest request = new ReportRequest();
        request.setResumeText("Skills: Java, Spring Boot, REST APIs, SQL, JUnit, Docker. "
                + "Experience: Built Java Spring Boot REST APIs with SQL persistence and JUnit tests for onboarding flows.");
        request.setJobDescription("Required: Java, Spring Boot, REST APIs, SQL, Kafka, Kubernetes, AWS, JUnit, CI/CD. "
                + "You will design distributed systems and own services end to end.");
        request.setExperienceLevel("threeToFive");
        ReportResponse created = reportService.createReport(request);
        publicId = created.getPublicId();
    }

    private String webhookBody(String referenceId, int amountPaise, String event) {
        return "{\"event\":\"" + event + "\",\"payload\":{\"payment_link\":{\"entity\":{"
                + "\"id\":\"plink_test_1\",\"reference_id\":\"" + referenceId + "\","
                + "\"amount\":" + amountPaise + ",\"amount_paid\":" + amountPaise + ",\"status\":\"paid\"}},"
                + "\"payment\":{\"entity\":{\"id\":\"pay_test_1\"}}}}";
    }

    private boolean deliver(String body) throws Exception {
        return paymentService.handleWebhook(body, WebhookSignatureVerifierTest.sign(body, SECRET));
    }

    private boolean isPaid() {
        return reportRepository.findByPublicId(java.util.UUID.fromString(publicId)).orElseThrow().isPaid();
    }

    @Test
    void aValidWebhookUnlocksTheReport() throws Exception {
        assertThat(isPaid()).isFalse();

        assertThat(deliver(webhookBody(publicId, 8900, "payment_link.paid"))).isTrue();

        assertThat(isPaid()).isTrue();
        assertThat(reportService.getReport(publicId).isFreePreview()).isFalse();
    }

    @Test
    void anInvalidSignatureIsRejectedAndUnlocksNothing() {
        String body = webhookBody(publicId, 8900, "payment_link.paid");

        assertThatThrownBy(() -> paymentService.handleWebhook(body, "deadbeef"))
                .isInstanceOf(PaymentService.InvalidWebhookSignatureException.class);

        assertThat(isPaid()).isFalse();
    }

    @Test
    void aMissingSignatureIsRejected() {
        String body = webhookBody(publicId, 8900, "payment_link.paid");

        assertThatThrownBy(() -> paymentService.handleWebhook(body, null))
                .isInstanceOf(PaymentService.InvalidWebhookSignatureException.class);

        assertThat(isPaid()).isFalse();
    }

    @Test
    void underpaymentDoesNotUnlockTheReport() throws Exception {
        // A hand-crafted link for one rupee must not buy an 89-rupee report.
        assertThat(deliver(webhookBody(publicId, 100, "payment_link.paid"))).isFalse();

        assertThat(isPaid()).isFalse();
    }

    @Test
    void otherProviderEventsAreIgnored() throws Exception {
        assertThat(deliver(webhookBody(publicId, 8900, "payment_link.expired"))).isFalse();

        assertThat(isPaid()).isFalse();
    }

    @Test
    void aWebhookForAnUnknownReportIsIgnored() throws Exception {
        assertThat(deliver(webhookBody("11111111-2222-3333-4444-555555555555", 8900, "payment_link.paid"))).isFalse();
    }

    @Test
    void aReplayedWebhookDoesNotUnlockTwice() throws Exception {
        assertThat(deliver(webhookBody(publicId, 8900, "payment_link.paid"))).isTrue();

        // Providers retry until they see a 2xx, so the same event will arrive again.
        assertThat(deliver(webhookBody(publicId, 8900, "payment_link.paid"))).isFalse();

        assertThat(isPaid()).isTrue();
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll().get(0).getStatus()).isEqualTo(Payment.STATUS_PAID);
    }

    @Test
    void creatingALinkStoresAnAuditRowAndReusesItOnASecondClick() {
        when(razorpayClient.createPaymentLink(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(new RazorpayClient.PaymentLink("plink_test_1", "https://rzp.io/i/abc123"));

        String first = paymentService.createPaymentLink(publicId);
        String second = paymentService.createPaymentLink(publicId);

        assertThat(first).isEqualTo("https://rzp.io/i/abc123");
        assertThat(second).isEqualTo(first);
        // Razorpay rejects a duplicate reference_id, and a double-click must not create two invoices.
        verify(razorpayClient).createPaymentLink(anyString(), anyInt(), anyString(), anyString());
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll().get(0).getAmountPaise()).isEqualTo(8900);
    }

    @Test
    void creatingALinkForAnAlreadyPaidReportIsRefused() throws Exception {
        deliver(webhookBody(publicId, 8900, "payment_link.paid"));

        assertThatThrownBy(() -> paymentService.createPaymentLink(publicId))
                .isInstanceOf(PaymentService.AlreadyPaidException.class);

        verify(razorpayClient, never()).createPaymentLink(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void creatingALinkForAnUnknownReportIsRefusedWithoutCallingTheProvider() {
        assertThatThrownBy(() -> paymentService.createPaymentLink("11111111-2222-3333-4444-555555555555"))
                .isInstanceOf(ReportNotFoundException.class);
        assertThatThrownBy(() -> paymentService.createPaymentLink("not-a-uuid"))
                .isInstanceOf(ReportNotFoundException.class);

        verify(razorpayClient, never()).createPaymentLink(anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void reportStaysLockedUntilAPaymentActuallyArrives() {
        when(razorpayClient.createPaymentLink(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(new RazorpayClient.PaymentLink("plink_test_1", "https://rzp.io/i/abc123"));

        paymentService.createPaymentLink(publicId);

        // Handing out a checkout URL grants nothing on its own.
        assertThat(isPaid()).isFalse();
        assertThat(reportService.getReport(publicId).isFreePreview()).isTrue();
        Report report = reportRepository.findByPublicId(java.util.UUID.fromString(publicId)).orElseThrow();
        assertThat(report.getPaidAt()).isNull();
    }
}
