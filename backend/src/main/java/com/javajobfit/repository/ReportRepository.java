package com.javajobfit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByPublicId(UUID publicId);
}
