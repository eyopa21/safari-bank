package org.example.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.domain.Payout;
import org.example.domain.PayoutRejectionReason;
import org.example.domain.PayoutResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PayoutResponse(
        PayoutResult result,
        @JsonProperty("bank_ref") String bankRef,
        PayoutRejectionReason reason) {

    public static PayoutResponse from(Payout p) {
        return new PayoutResponse(p.getResult(), p.getBankRef(), p.getReason());
    }
}
