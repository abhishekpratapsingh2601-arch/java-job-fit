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

import com.javajobfit.api.dto.FeedbackRequest;
import com.javajobfit.domain.Feedback;
import com.javajobfit.repository.FeedbackRepository;
import com.javajobfit.repository.ReportRepository;
import com.javajobfit.service.ReportNotFoundException;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;

    public FeedbackController(FeedbackRepository feedbackRepository, ReportRepository reportRepository) {
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        if (request.getReportId() != null && !reportRepository.existsById(request.getReportId())) {
            throw new ReportNotFoundException(request.getReportId());
        }

        Feedback feedback = new Feedback();
        feedback.setReportId(request.getReportId());
        String email = request.getEmail();
        feedback.setEmail(email == null || email.isBlank() ? null : email.trim());
        feedback.setMessage(request.getMessage());
        Feedback saved = feedbackRepository.save(feedback);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", saved.getId()));
    }
}
