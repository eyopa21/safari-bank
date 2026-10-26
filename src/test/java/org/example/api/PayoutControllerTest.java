package org.example.api;

import org.example.domain.Payout;
import org.example.domain.PayoutRejectionReason;
import org.example.domain.PayoutStatus;
import org.example.service.PayoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayoutController.class)
class PayoutControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PayoutService service;

    @Test
    void acceptedResponseHasBankRef() throws Exception {
        when(service.process(eq("P-7f3a91"), eq("ACC-001"), any()))
                .thenReturn(Payout.accepted("P-7f3a91", "ACC-001", new BigDecimal("5000"), "BR4F830B87B25B62A5", PayoutStatus.SUCCEEDED, "ev"));

        mvc.perform(post("/bank/payouts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payout_id\":\"P-7f3a91\",\"bank_account\":\"ACC-001\",\"amount\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ACCEPTED"))
                .andExpect(jsonPath("$.bank_ref").value("BR4F830B87B25B62A5"))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    void rejectedResponseHasReasonAndStill200() throws Exception {
        when(service.process(any(), any(), any()))
                .thenReturn(Payout.rejected("P-1", "XYZ", BigDecimal.TEN, PayoutRejectionReason.BENEFICIARY_ACCOUNT_INVALID));

        mvc.perform(post("/bank/payouts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payout_id\":\"P-1\",\"bank_account\":\"XYZ\",\"amount\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("BENEFICIARY_ACCOUNT_INVALID"))
                .andExpect(jsonPath("$.bank_ref").doesNotExist());
    }

    @Test
    void missingFieldIs400() throws Exception {
        mvc.perform(post("/bank/payouts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payout_id\":\"P-1\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusEndpointReturnsStatusOnly() throws Exception {
        when(service.status("P-7f3a91")).thenReturn(PayoutStatus.SUCCEEDED);
        when(service.status("nope")).thenReturn(PayoutStatus.NOT_FOUND);

        mvc.perform(get("/bank/payouts/P-7f3a91")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"));
        mvc.perform(get("/bank/payouts/nope")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }
}
