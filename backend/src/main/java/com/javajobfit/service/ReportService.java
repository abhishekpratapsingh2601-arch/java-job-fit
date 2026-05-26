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
    private static final List<String> PREMIUM_LOCKED_SECTIONS = Arrays.asList(
            "Full keyword analysis",
            "10+ resume bullet upgrades",
            "Keyword placement suggestions",
            "Tailored Java/Spring Boot resume summary",
            "Full Java interview question set",
            "Full 7-day prep plan",
            "Cover letter draft",
            "LinkedIn headline/About rewrite",
            "Export full PDF/DOCX report");

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
        report.markRawInputsNotStored();
        report.setExperienceLevel(request.getExperienceLevel());
        report.setScore(result.getScore());
        report.setScoreSummary(result.getScoreSummary());
        report.setMatchedSkills(join(result.getMatchedSkills()));
        report.setMissingKeywords(join(result.getMissingKeywords()));
        report.setTopFixes(join(result.getTopFixes()));
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
        List<String> matchedSkills = split(report.getMatchedSkills());
        List<String> missingKeywords = split(report.getMissingKeywords());
        List<String> bulletSuggestions = split(report.getBulletSuggestions());
        List<String> interviewQuestions = split(report.getInterviewQuestions());
        List<String> prepPlan = split(report.getPrepPlan());

        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReportId(report.getId());
        response.setScore(report.getScore());
        response.setAtsScore(report.getScore());
        response.setScoreSummary(report.getScoreSummary());
        response.setMatchedSkills(limit(matchedSkills, 3));
        response.setMatchedStrengths(limit(matchedSkills, 3));
        response.setMissingKeywords(limit(missingKeywords, 5));
        response.setTopFixes(limit(split(report.getTopFixes()), 3));
        response.setBulletSuggestions(limit(bulletSuggestions, 1));
        response.setBulletUpgrades(limit(bulletSuggestions, 1));
        response.setInterviewQuestions(limit(interviewQuestions, 3));
        response.setPrepPlan(limit(prepPlan, 2));
        response.setFreePreview(true);
        response.setPremiumAvailable(true);
        response.setPremiumLockedSections(PREMIUM_LOCKED_SECTIONS);
        response.setExperienceLevel(report.getExperienceLevel());
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

    private List<String> limit(List<String> values, int maxItems) {
        return values.stream().limit(maxItems).collect(Collectors.toList());
    }
}
