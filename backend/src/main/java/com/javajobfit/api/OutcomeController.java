package com.javajobfit.api;

import java.util.Collections;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javajobfit.api.dto.OutcomeRequest;
import com.javajobfit.domain.Outcome;
import com.javajobfit.repository.OutcomeRepository;
import com.javajobfit.service.ReportService;

@RestController
@RequestMapping("/api/outcomes")
public class OutcomeController {
    private final OutcomeRepository outcomeRepository;
    private final ReportService reportService;

    public OutcomeController(OutcomeRepository outcomeRepository, ReportService reportService) {
        this.outcomeRepository = outcomeRepository;
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> saveOutcome(@Valid @RequestBody OutcomeRequest request) {
        Outcome outcome = new Outcome();
        outcome.setReportId(reportService.resolveInternalReportId(null, request.getReportPublicId()));
        outcome.setOutcomeType(trim(request.getOutcomeType()));
        outcome.setUsefulnessRating(request.getUsefulnessRating());
        outcome.setGotRecruiterReply(request.getGotRecruiterReply());
        outcome.setGotInterview(request.getGotInterview());
        outcome.setAppliedWithThisResume(request.getAppliedWithThisResume());
        outcome.setMessage(trimToNull(request.getMessage()));
        outcome.setEmail(trimToNull(request.getEmail()));
        Outcome saved = outcomeRepository.save(outcome);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", saved.getId()));
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
