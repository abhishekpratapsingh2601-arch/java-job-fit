package com.javajobfit.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.score").isNumber())
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
}
