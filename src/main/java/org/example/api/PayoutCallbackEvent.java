package org.example.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.domain.PayoutStatus;

public record PayoutCallbackEvent(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("payout_id") String payoutId,
        @JsonProperty("bank_ref") String bankRef,
        PayoutStatus status) {
}
