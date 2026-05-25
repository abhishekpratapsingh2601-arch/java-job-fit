package com.javajobfit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerTest {
    @Autowired
    private MockMvc mockMvc;

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
}
