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

import com.javajobfit.api.dto.LeadRequest;
import com.javajobfit.domain.Lead;
import com.javajobfit.repository.LeadRepository;
import com.javajobfit.service.ReportService;

@RestController
@RequestMapping("/api/leads")
public class LeadController {
    private final LeadRepository leadRepository;
    private final ReportService reportService;

    public LeadController(LeadRepository leadRepository, ReportService reportService) {
        this.leadRepository = leadRepository;
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> saveLead(@Valid @RequestBody LeadRequest request) {
        Lead lead = new Lead();
        lead.setEmail(request.getEmail().trim());
        lead.setExperienceLevel(blankToNull(request.getExperienceLevel()));
        lead.setCountry(blankToNull(request.getCountry()));
        lead.setReportId(reportService.resolveInternalReportId(request.getReportId(), request.getPublicId()));
        lead.setConsent(request.isConsent());
        lead.setSource("scan_result");

        Lead saved = leadRepository.save(lead);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", saved.getId()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
