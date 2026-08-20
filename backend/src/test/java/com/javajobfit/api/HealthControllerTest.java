package com.javajobfit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.javajobfit.repository.EventRepository;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanUp() {
        eventRepository.deleteAll();
    }

    @Test
    void dbHealthWritesOneThrottledKeepaliveEvent() throws Exception {
        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("reachable"))
                .andExpect(jsonPath("$.keepalive").value("written"));

        // Second call inside the throttle window must not write another row.
        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keepalive").value("skipped"));

        assertThat(eventRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getEventName()).isEqualTo("db_keepalive");
                    assertThat(event.getSource()).isEqualTo("keep_awake");
                });
    }
}
