package com.javajobfit.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.javajobfit.repository.ReportRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void cleanUp() {
        reportRepository.deleteAll();
    }

    @Test
    void rootEndpointReturnsApiStatus() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("JavaJobFit API"))
                .andExpect(jsonPath("$.health").value("/api/health"));
    }

    @Test
    void createReportReturnsGeneratedReportAndDoesNotExposeRawResume() throws Exception {
        String rawResume = "Private phone 9999999999 Java Spring Boot SQL";

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"resumeText\":\"" + rawResume + "\","
                                + "\"jobDescription\":\"Hiring Java Spring Boot REST SQL developer\","
                                + "\"experienceLevel\":\"oneToThree\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reportId").isNumber())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.atsScore").isNumber())
                .andExpect(jsonPath("$.scoreSummary").exists())
                .andExpect(jsonPath("$.topFixes").isArray())
                .andExpect(jsonPath("$.freePreview").value(true))
                .andExpect(jsonPath("$.premiumAvailable").value(true))
                .andExpect(content().string(not(containsString("9999999999"))));

        reportRepository.findAll().forEach(report -> {
            org.assertj.core.api.Assertions.assertThat(report.getResumeText()).isEqualTo("[not stored for privacy]");
            org.assertj.core.api.Assertions.assertThat(report.getJobDescription()).isEqualTo("[not stored for privacy]");
        });
    }

    @Test
    void createReportRejectsInvalidExperienceLevel() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"resumeText\":\"Java\","
                                + "\"jobDescription\":\"Spring Boot\","
                                + "\"experienceLevel\":\"invalid\""
                                + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReportReturnsSavedReport() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"resumeText\":\"Java Spring Boot SQL JUnit REST API backend project with testing\","
                                + "\"jobDescription\":\"Hiring Java Spring Boot REST SQL developer\","
                                + "\"experienceLevel\":\"oneToThree\""
                                + "}"))
                .andExpect(status().isCreated());

        Long savedId = reportRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/reports/{id}", savedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedId))
                .andExpect(jsonPath("$.reportId").value(savedId))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.atsScore").isNumber())
                .andExpect(jsonPath("$.scoreSummary").exists())
                .andExpect(jsonPath("$.matchedSkills").isArray())
                .andExpect(jsonPath("$.matchedStrengths").isArray())
                .andExpect(jsonPath("$.missingKeywords").isArray())
                .andExpect(jsonPath("$.topFixes").isArray())
                .andExpect(jsonPath("$.bulletSuggestions").isArray())
                .andExpect(jsonPath("$.bulletUpgrades").isArray())
                .andExpect(jsonPath("$.interviewQuestions").isArray())
                .andExpect(jsonPath("$.prepPlan").isArray())
                .andExpect(jsonPath("$.freePreview").value(true))
                .andExpect(jsonPath("$.premiumAvailable").value(true))
                .andExpect(jsonPath("$.experienceLevel").value("oneToThree"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createReportReturnsOnlyFreePreviewContent() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"resumeText\":\"Java Spring Boot SQL JUnit Mockito Docker Git Maven REST API backend project\","
                                + "\"jobDescription\":\"Hiring Java Spring Boot REST SQL Kafka Docker CI/CD Kubernetes AWS Mockito JUnit microservices developer\","
                                + "\"experienceLevel\":\"threeToFive\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchedSkills.length()").value(3))
                .andExpect(jsonPath("$.missingKeywords.length()").value(5))
                .andExpect(jsonPath("$.topFixes.length()").value(3))
                .andExpect(jsonPath("$.bulletSuggestions.length()").value(1))
                .andExpect(jsonPath("$.interviewQuestions.length()").value(3))
                .andExpect(jsonPath("$.prepPlan.length()").value(2))
                .andExpect(jsonPath("$.premiumLockedSections").isArray())
                .andExpect(content().string(not(containsString("Day 7:"))));
    }

    @Test
    void getReportReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/reports/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Report not found: 999999"));
    }
}
