package com.javajobfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
