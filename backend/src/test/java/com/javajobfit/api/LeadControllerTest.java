package com.javajobfit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.javajobfit.domain.Report;
import com.javajobfit.repository.LeadRepository;
import com.javajobfit.repository.ReportRepository;

@SpringBootTest
@AutoConfigureMockMvc
class LeadControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void cleanUp() {
        leadRepository.deleteAll();
        reportRepository.deleteAll();
    }

    @Test
    void leadCaptureSavesScanResultLeadWithoutRawResumeOrJobDescription() throws Exception {
        Report report = reportRepository.save(buildReport());
        Long reportId = report.getId();
        String publicId = report.getPublicId().toString();

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"email\":\"lead@example.com\","
                                + "\"experienceLevel\":\"threeToFive\","
                                + "\"country\":\"India\","
                                + "\"publicId\":\"" + publicId + "\","
                                + "\"consent\":true"
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        org.assertj.core.api.Assertions.assertThat(leadRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    org.assertj.core.api.Assertions.assertThat(saved.getEmail()).isEqualTo("lead@example.com");
                    org.assertj.core.api.Assertions.assertThat(saved.getExperienceLevel()).isEqualTo("threeToFive");
                    org.assertj.core.api.Assertions.assertThat(saved.getCountry()).isEqualTo("India");
                    org.assertj.core.api.Assertions.assertThat(saved.getReportId()).isEqualTo(reportId);
                    org.assertj.core.api.Assertions.assertThat(saved.isConsent()).isTrue();
                    org.assertj.core.api.Assertions.assertThat(saved.getSource()).isEqualTo("scan_result");
                });
    }

    @Test
    void leadCaptureRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"email\":\"not-an-email\","
                                + "\"consent\":true"
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Please enter a valid email address."));
    }

    @Test
    void leadCaptureRejectsMissingConsent() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"email\":\"lead@example.com\","
                                + "\"consent\":false"
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.consent")
                        .value("Please agree to receive JavaJobFit product updates before saving your email."));

        org.assertj.core.api.Assertions.assertThat(leadRepository.findAll()).isEmpty();
    }

    @Test
    void leadCaptureRejectsUnknownReportId() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"email\":\"lead@example.com\","
                                + "\"publicId\":\"00000000-0000-0000-0000-000000000000\","
                                + "\"consent\":true"
                                + "}"))
                .andExpect(status().isNotFound());
    }

    private Report buildReport() {
        Report report = new Report();
        report.setExperienceLevel("threeToFive");
        report.setScore(72);
        report.setScoreSummary("Useful Java match with gaps.");
        report.setMatchedSkills("Java");
        report.setMissingKeywords("Kafka");
        report.setTopFixes("Add Kafka proof.");
        report.setBulletSuggestions("Improve bullet.");
        report.setInterviewQuestions("Explain Spring Boot.");
        report.setPrepPlan("Day 1.");
        return report;
    }
}
