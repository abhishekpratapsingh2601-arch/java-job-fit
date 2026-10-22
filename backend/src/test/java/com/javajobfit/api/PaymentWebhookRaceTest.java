package com.javajobfit.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.javajobfit.service.PaymentService;

/** Two copies of one webhook racing to insert the same payment row. */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentWebhookRaceTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void theLoserOfAConcurrentInsertIsAnsweredAsAReplayNotA500() throws Exception {
        when(paymentService.handleWebhook(anyString(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key idx_payments_payment_link_id"))
                .thenReturn(false);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sig")
                        .content("{\"event\":\"payment_link.paid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlocked").value(false));
        verify(paymentService, times(2)).handleWebhook(anyString(), any());
    }

    @Test
    void aPersistentIntegrityProblemStillFailsSoRazorpayRetries() throws Exception {
        when(paymentService.handleWebhook(anyString(), any()))
                .thenThrow(new DataIntegrityViolationException("not null violation"));

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sig")
                        .content("{\"event\":\"payment_link.paid\"}"))
                .andExpect(status().isInternalServerError());
        verify(paymentService, times(2)).handleWebhook(anyString(), any());
    }
}
