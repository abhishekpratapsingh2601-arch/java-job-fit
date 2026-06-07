package com.javajobfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajobfit.domain.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}
