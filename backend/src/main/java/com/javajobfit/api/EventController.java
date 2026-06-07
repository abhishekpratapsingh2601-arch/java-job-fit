package com.javajobfit.api;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javajobfit.api.dto.EventRequest;
import com.javajobfit.domain.Event;
import com.javajobfit.repository.EventRepository;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> trackEvent(@Valid @RequestBody EventRequest request) {
        Event event = new Event();
        event.setEventName(trim(request.getEventName()));
        event.setPagePath(normalizePath(request.getPagePath()));
        event.setReportPublicId(parseUuid(request.getReportPublicId()));
        event.setExperienceLevel(trimToNull(request.getExperienceLevel()));
        event.setCountry(trimToNull(request.getCountry()));
        event.setSource(trimToNull(request.getSource()));
        event.setUtmSource(trimToNull(request.getUtmSource()));
        event.setUtmMedium(trimToNull(request.getUtmMedium()));
        event.setUtmCampaign(trimToNull(request.getUtmCampaign()));
        event.setMetadataJson(null);

        Event saved = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", saved.getId()));
    }

    private UUID parseUuid(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : UUID.fromString(trimmed);
    }

    private String normalizePath(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        int queryIndex = trimmed.indexOf('?');
        return queryIndex >= 0 ? trimmed.substring(0, queryIndex) : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
