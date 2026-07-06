package com.javajobfit.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {
    private static final String ORIGIN = "http://localhost:4173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightAllowsCredentialsSoSendBeaconAnalyticsAreNotBlocked() throws Exception {
        // Reproduces the browser preflight that navigator.sendBeacon triggers for a
        // cross-origin application/json POST. Without Access-Control-Allow-Credentials: true
        // the browser blocks the request (the /api/events CORS failure).
        mockMvc.perform(options("/api/events")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void actualPostResponseCarriesCredentialedCorsHeaders() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .contentType("application/json")
                        .content("{\"eventName\":\"scan_completed\"}"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
