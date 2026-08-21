package com.javajobfit.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.javajobfit.api.dto.ReportRequest;
import com.javajobfit.api.dto.ReportResponse;
import com.javajobfit.domain.Report;
import com.javajobfit.repository.ReportRepository;

@SpringBootTest
class ReportPaywallTest {
    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    private ReportRequest request() {
        ReportRequest request = new ReportRequest();
        request.setResumeText("Senior Java developer. Skills: Java, Spring Boot, REST APIs, SQL, JUnit, Docker. "
                + "Experience: Built Spring Boot REST APIs with PostgreSQL persistence, optimized queries, "
                + "wrote JUnit and Mockito tests, containerized services with Docker and mentored juniors.");
        request.setJobDescription("Required: Java, Spring Boot, REST APIs, PostgreSQL, Kafka, Kubernetes, AWS, "
                + "microservices, CI/CD, JUnit. Redis and GraphQL are a plus. You will design distributed "
                + "systems, mentor developers and own services end to end.");
        request.setExperienceLevel("threeToFive");
        return request;
    }

    @Test
    void freeReportIsTruncatedAndAdvertisesLockedSections() {
        ReportResponse free = reportService.createReport(request());

        assertThat(free.isFreePreview()).isTrue();
        assertThat(free.isPremiumAvailable()).isTrue();
        assertThat(free.getPremiumLockedSections()).isNotEmpty();
        assertThat(free.getMatchedSkills()).hasSizeLessThanOrEqualTo(3);
        assertThat(free.getMissingKeywords()).hasSizeLessThanOrEqualTo(5);
        assertThat(free.getTopFixes()).hasSizeLessThanOrEqualTo(3);
        assertThat(free.getBulletSuggestions()).hasSizeLessThanOrEqualTo(1);
        assertThat(free.getInterviewQuestions()).hasSizeLessThanOrEqualTo(3);
        assertThat(free.getPrepPlan()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void paidReportReturnsEverythingThatWasGeneratedAndDropsTheUpsell() {
        ReportResponse free = reportService.createReport(request());
        Report stored = reportRepository.findByPublicId(java.util.UUID.fromString(free.getPublicId())).orElseThrow();

        // Count what the engine actually generated, so the assertions below prove the paid
        // response is un-truncated rather than merely "bigger".
        int generatedQuestions = stored.getInterviewQuestions().split("\n---ITEM---\n").length;
        int generatedPlan = stored.getPrepPlan().split("\n---ITEM---\n").length;

        stored.markPaid();
        reportRepository.save(stored);

        ReportResponse paid = reportService.getReport(free.getPublicId());

        assertThat(paid.isFreePreview()).isFalse();
        assertThat(paid.isPremiumAvailable()).isFalse();
        assertThat(paid.getPremiumLockedSections()).isEmpty();
        assertThat(paid.getInterviewQuestions()).hasSize(generatedQuestions);
        assertThat(paid.getPrepPlan()).hasSize(generatedPlan);
        assertThat(paid.getInterviewQuestions().size()).isGreaterThan(free.getInterviewQuestions().size());
        assertThat(paid.getScore()).isEqualTo(free.getScore());
    }

    @Test
    void newReportsAreNeverPaidOnCreation() {
        ReportResponse created = reportService.createReport(request());
        Report stored = reportRepository.findByPublicId(java.util.UUID.fromString(created.getPublicId())).orElseThrow();

        assertThat(stored.isPaid()).isFalse();
        assertThat(stored.getPaidAt()).isNull();
    }

    @Test
    void markPaidRecordsWhenItHappened() {
        ReportResponse created = reportService.createReport(request());
        Report stored = reportRepository.findByPublicId(java.util.UUID.fromString(created.getPublicId())).orElseThrow();

        stored.markPaid();

        assertThat(stored.isPaid()).isTrue();
        assertThat(stored.getPaidAt()).isNotNull();
    }
}
