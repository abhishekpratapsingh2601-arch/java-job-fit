package com.javajobfit.domain;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * One row per payment link we hand out, updated in place when the provider confirms payment.
 * Holds no personal data — ids, amount, status and timestamps only.
 */
@Entity
@Table(name = "payments")
public class Payment {
    public static final String STATUS_CREATED = "created";
    public static final String STATUS_PAID = "paid";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_public_id", nullable = false, columnDefinition = "UUID")
    private UUID reportPublicId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "payment_link_url")
    private String paymentLinkUrl;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getReportPublicId() {
        return reportPublicId;
    }

    public void setReportPublicId(UUID reportPublicId) {
        this.reportPublicId = reportPublicId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public void setPaymentLinkId(String paymentLinkId) {
        this.paymentLinkId = paymentLinkId;
    }

    public String getPaymentLinkUrl() {
        return paymentLinkUrl;
    }

    public void setPaymentLinkUrl(String paymentLinkUrl) {
        this.paymentLinkUrl = paymentLinkUrl;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public int getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(int amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    /** Records a confirmed payment. Only ever called after webhook signature verification. */
    public void markPaid(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
        this.status = STATUS_PAID;
        this.paidAt = Instant.now();
    }
}
