package org.example.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayoutRequest(
        @JsonProperty("payout_id") @NotBlank String payoutId,
        @JsonProperty("bank_account") @NotBlank String bankAccount,
        @NotNull BigDecimal amount) {
}
