package com.javajobfit.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.javajobfit.api.dto.ReportRequest;
import com.javajobfit.api.dto.ReportResponse;
import com.javajobfit.domain.Report;
import com.javajobfit.repository.ReportRepository;

@Service
public class ReportService {
    private static final String LIST_SEPARATOR = "\n---ITEM---\n";

    private final AnalysisService analysisService;
    private final ReportRepository reportRepository;

    public ReportService(AnalysisService analysisService, ReportRepository reportRepository) {
        this.analysisService = analysisService;
        this.reportRepository = reportRepository;
    }

    public ReportResponse createReport(ReportRequest request) {
        AnalysisResult result = analysisService.analyze(
                request.getResumeText(),
                request.getJobDescription(),
                request.getExperienceLevel());

        Report report = new Report();
        report.setResumeText(request.getResumeText());
        report.setJobDescription(request.getJobDescription());
        report.setExperienceLevel(request.getExperienceLevel());
        report.setScore(result.getScore());
        report.setMatchedSkills(join(result.getMatchedSkills()));
        report.setMissingKeywords(join(result.getMissingKeywords()));
        report.setBulletSuggestions(join(result.getBulletSuggestions()));
        report.setInterviewQuestions(join(result.getInterviewQuestions()));
        report.setPrepPlan(join(result.getPrepPlan()));

        return toResponse(reportRepository.save(report));
    }

    public ReportResponse getReport(Long id) {
        return reportRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    private ReportResponse toResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setScore(report.getScore());
        response.setMatchedSkills(split(report.getMatchedSkills()));
        response.setMissingKeywords(split(report.getMissingKeywords()));
        response.setBulletSuggestions(split(report.getBulletSuggestions()));
        response.setInterviewQuestions(split(report.getInterviewQuestions()));
        response.setPrepPlan(split(report.getPrepPlan()));
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private String join(List<String> values) {
        return String.join(LIST_SEPARATOR, values);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(LIST_SEPARATOR))
                .collect(Collectors.toList());
    }
}
