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
import com.javajobfit.repository.FeedbackRepository;
import com.javajobfit.repository.ReportRepository;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void cleanUp() {
        feedbackRepository.deleteAll();
        reportRepository.deleteAll();
    }

    @Test
    void feedbackRejectsMissingReportId() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"reportId\":999999,"
                                + "\"email\":\"user@example.com\","
                                + "\"message\":\"Helpful report\""
                                + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void feedbackRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"email\":\"not-an-email\","
                                + "\"message\":\"Helpful report\""
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void feedbackRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"email\":\"user@example.com\","
                                + "\"message\":\"\""
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.message").value("Feedback message is required."));
    }

    @Test
    void feedbackSavesWhenLinkedToExistingReport() throws Exception {
        Report report = new Report();
        report.setExperienceLevel("oneToThree");
        report.setScore(82);
        report.setMatchedSkills("Java");
        report.setMissingKeywords("Kafka");
        report.setBulletSuggestions("Bullet");
        report.setInterviewQuestions("Question");
        report.setPrepPlan("Plan");
        Long reportId = reportRepository.save(report).getId();

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"reportId\":" + reportId + ","
                                + "\"email\":\"user@example.com\","
                                + "\"message\":\"Please add a resume download option\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        org.assertj.core.api.Assertions.assertThat(feedbackRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    org.assertj.core.api.Assertions.assertThat(saved.getReportId()).isEqualTo(reportId);
                    org.assertj.core.api.Assertions.assertThat(saved.getEmail()).isEqualTo("user@example.com");
                    org.assertj.core.api.Assertions.assertThat(saved.getMessage())
                            .isEqualTo("Please add a resume download option");
                });
    }
}
