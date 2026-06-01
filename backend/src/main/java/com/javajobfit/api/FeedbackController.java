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
import com.javajobfit.service.ReportService;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackRepository feedbackRepository;
    private final ReportService reportService;

    public FeedbackController(FeedbackRepository feedbackRepository, ReportService reportService) {
        this.feedbackRepository = feedbackRepository;
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setReportId(reportService.resolveInternalReportId(request.getReportId(), request.getPublicId()));
        String email = request.getEmail();
        feedback.setEmail(email == null || email.isBlank() ? null : email.trim());
        feedback.setMessage(request.getMessage());
        Feedback saved = feedbackRepository.save(feedback);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", saved.getId()));
    }
}
