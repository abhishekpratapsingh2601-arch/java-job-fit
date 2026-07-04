package com.javajobfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Outcome;

public interface OutcomeRepository extends JpaRepository<Outcome, Long> {
}
