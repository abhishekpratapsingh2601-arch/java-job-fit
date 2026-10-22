package com.javajobfit.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class RequestHardeningTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void anOversizedJsonBodyIsRefusedBeforeItIsParsed() throws Exception {
        String huge = "x".repeat((int) RequestBodyLimitFilter.MAX_BODY_BYTES + 1);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeText\":\"" + huge + "\"}"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("Request is too large."))
                // The headers filter runs first, so even a rejection carries them.
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(huge))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void aMaximumSizeReportRequestStillFitsUnderTheCap() throws Exception {
        // Two 12,000-character fields of 3-byte UTF-8 characters is the largest valid request.
        String longField = "₹".repeat(12000);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeText\":\"" + longField + "\",\"jobDescription\":\"" + longField + "\"}"))
                .andExpect(status().is(org.hamcrest.Matchers.not(413)));
    }

    @Test
    void apiResponsesCarryBaselineSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
