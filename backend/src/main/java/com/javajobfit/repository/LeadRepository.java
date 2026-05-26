package com.javajobfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {
}
