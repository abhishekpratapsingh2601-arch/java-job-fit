package com.javajobfit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.javajobfit.service.RazorpayClient;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RazorpayClient razorpayClient;

    @Test
    void configReportsPaymentsDisabledByDefaultSoTheCtaCanHideItself() throws Exception {
        mockMvc.perform(get("/api/payments/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.priceInr").value(89))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void checkoutIsUnavailableWhileTheProviderIsDisabled() throws Exception {
        mockMvc.perform(post("/api/payments/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicId\":\"11111111-2222-3333-4444-555555555555\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void webhookWithoutAValidSignatureIsUnauthorizedAndLeaksNoDetail() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "deadbeef")
                        .content("{\"event\":\"payment_link.paid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid signature."));
    }

    @Test
    void webhookWithNoSignatureHeaderAtAllIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment_link.paid\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutRequestWithoutAReportReferenceIsRejected() throws Exception {
        mockMvc.perform(post("/api/payments/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
