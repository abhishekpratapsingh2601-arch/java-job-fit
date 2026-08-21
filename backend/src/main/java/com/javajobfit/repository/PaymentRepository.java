package com.javajobfit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentLinkId(String paymentLinkId);

    Optional<Payment> findFirstByReportPublicIdAndStatusOrderByIdDesc(UUID reportPublicId, String status);
}
