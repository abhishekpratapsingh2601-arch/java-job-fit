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

import com.javajobfit.repository.EventRepository;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanUp() {
        eventRepository.deleteAll();
    }

    @Test
    void eventCaptureStoresOnlySafeEventMetadata() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"eventName\":\"scan_completed\","
                                + "\"pagePath\":\"/java-job-fit/index.html?private=ignored\","
                                + "\"reportPublicId\":\"00000000-0000-0000-0000-000000000001\","
                                + "\"experienceLevel\":\"oneToThree\","
                                + "\"country\":\"India\","
                                + "\"source\":\"frontend\","
                                + "\"utmSource\":\"linkedin\","
                                + "\"utmMedium\":\"social\","
                                + "\"utmCampaign\":\"beta\""
                                + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        org.assertj.core.api.Assertions.assertThat(eventRepository.findAll())
                .singleElement()
                .satisfies(saved -> {
                    org.assertj.core.api.Assertions.assertThat(saved.getEventName()).isEqualTo("scan_completed");
                    org.assertj.core.api.Assertions.assertThat(saved.getPagePath()).isEqualTo("/java-job-fit/index.html");
                    org.assertj.core.api.Assertions.assertThat(saved.getReportPublicId()).hasToString("00000000-0000-0000-0000-000000000001");
                    org.assertj.core.api.Assertions.assertThat(saved.getExperienceLevel()).isEqualTo("oneToThree");
                    org.assertj.core.api.Assertions.assertThat(saved.getCountry()).isEqualTo("India");
                    org.assertj.core.api.Assertions.assertThat(saved.getMetadataJson()).isNull();
                });
    }

    @Test
    void eventCaptureRejectsInvalidEventName() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"Scan Completed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.eventName").exists());
    }
}
