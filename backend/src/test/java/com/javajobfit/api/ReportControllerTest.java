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
    void healthEndpointReturnsSafePayload() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("JavaJobFit API"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(content().string(not(containsString("DATABASE_URL"))))
                .andExpect(content().string(not(containsString("PASSWORD"))));
    }

    @Test
    void databaseHealthEndpointReturnsSafePayload() throws Exception {
        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("JavaJobFit API"))
                .andExpect(jsonPath("$.database").value("reachable"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(content().string(not(containsString("jdbc:"))))
                .andExpect(content().string(not(containsString("DATABASE_URL"))))
                .andExpect(content().string(not(containsString("PASSWORD"))));
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
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.reportId").isString())
                .andExpect(jsonPath("$.publicId").isString())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.atsScore").isNumber())
                .andExpect(jsonPath("$.scoreSummary").exists())
                .andExpect(jsonPath("$.topFixes").isArray())
                .andExpect(jsonPath("$.freePreview").value(true))
                .andExpect(jsonPath("$.premiumAvailable").value(true))
                .andExpect(content().string(not(containsString("9999999999"))));

        org.assertj.core.api.Assertions.assertThat(reportRepository.findAll())
                .singleElement()
                .satisfies(report -> {
                    org.assertj.core.api.Assertions.assertThat(report.getMatchedSkills()).contains("Java");
                    org.assertj.core.api.Assertions.assertThat(report.getExperienceLevel()).isEqualTo("oneToThree");
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
    void createReportRejectsEmptyInputWithoutEchoingRawText() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"resumeText\":\"\","
                                + "\"jobDescription\":\"\","
                                + "\"experienceLevel\":\"oneToThree\""
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please provide valid resume text, job description, and experience level."))
                .andExpect(jsonPath("$.fields.resumeText").value("Resume is required."))
                .andExpect(jsonPath("$.fields.jobDescription").value("Job description is required."));
    }

    @Test
    void createReportRejectsTooShortInputWithoutEchoingRawText() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"resumeText\":\"Private phone 9999999999\","
                                + "\"jobDescription\":\"Secret target role text\","
                                + "\"experienceLevel\":\"oneToThree\""
                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.resumeText").value("Please paste a longer resume for a useful scan."))
                .andExpect(jsonPath("$.fields.jobDescription").value("Please paste a longer job description for a useful scan."))
                .andExpect(content().string(not(containsString("9999999999"))))
                .andExpect(content().string(not(containsString("Secret target role text"))));
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
        String publicId = reportRepository.findAll().get(0).getPublicId().toString();

        mockMvc.perform(get("/api/reports/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.reportId").value(publicId))
                .andExpect(jsonPath("$.publicId").value(publicId))
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

        mockMvc.perform(get("/api/reports/{id}", savedId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Report not found."));
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
                .andExpect(jsonPath("$.error").value("Report not found."));
    }

    @Test
    void getReportReturnsNotFoundForInvalidPublicId() throws Exception {
        mockMvc.perform(get("/api/reports/{id}", "not-a-public-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Report not found."));
    }
}
