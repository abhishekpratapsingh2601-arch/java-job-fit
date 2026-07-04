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
import com.javajobfit.repository.OutcomeRepository;
import com.javajobfit.repository.ReportRepository;

@SpringBootTest
@AutoConfigureMockMvc
class OutcomeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutcomeRepository outcomeRepository;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void cleanUp() {
        outcomeRepository.deleteAll();
        reportRepository.deleteAll();
    }

    @Test
    void savesOutcomeUsingReportPublicIdWithoutRawResumeOrJobDescription() throws Exception {
        Report report = reportRepository.save(buildReport());
        String publicId = report.getPublicId().toString();

        mockMvc.perform(post("/api/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"reportPublicId\":\"" + publicId + "\","
                                + "\"outcomeType\":\"useful\","
                                + "\"usefulnessRating\":5,"
                                + "\"gotRecruiterReply\":true,"
                                + "\"gotInterview\":false,"
                                + "\"appliedWithThisResume\":true,"
                                + "\"message\":\"Helpful report\","
                                + "\"email\":\"user@example.com\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        org.assertj.core.api.Assertions.assertThat(outcomeRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    org.assertj.core.api.Assertions.assertThat(saved.getReportId()).isEqualTo(report.getId());
                    org.assertj.core.api.Assertions.assertThat(saved.getOutcomeType()).isEqualTo("useful");
                    org.assertj.core.api.Assertions.assertThat(saved.getUsefulnessRating()).isEqualTo(5);
                    org.assertj.core.api.Assertions.assertThat(saved.getMessage()).isEqualTo("Helpful report");
                    org.assertj.core.api.Assertions.assertThat(saved.getEmail()).isEqualTo("user@example.com");
                });
    }

    @Test
    void rejectsInvalidOutcomeType() throws Exception {
        mockMvc.perform(post("/api/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"outcomeType\":\"payment_success\","
                                + "\"usefulnessRating\":5"
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.outcomeType").exists());

        org.assertj.core.api.Assertions.assertThat(outcomeRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsUnknownReportPublicId() throws Exception {
        mockMvc.perform(post("/api/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"reportPublicId\":\"00000000-0000-0000-0000-000000000000\","
                                + "\"outcomeType\":\"useful\","
                                + "\"usefulnessRating\":5"
                                + "}"))
                .andExpect(status().isNotFound());
    }

    private Report buildReport() {
        Report report = new Report();
        report.setExperienceLevel("oneToThree");
        report.setScore(81);
        report.setScoreSummary("Strong Java fit.");
        report.setMatchedSkills("Java");
        report.setMissingKeywords("Kafka");
        report.setTopFixes("Add Kafka proof.");
        report.setBulletSuggestions("Improve bullet.");
        report.setInterviewQuestions("Explain Spring Boot.");
        report.setPrepPlan("Day 1.");
        return report;
    }
}
