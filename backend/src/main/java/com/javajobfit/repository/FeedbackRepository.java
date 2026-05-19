package com.javajobfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
