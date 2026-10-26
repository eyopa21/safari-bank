package org.example.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayoutTest {

    @Test
    void acceptedStartsInProgressAndSettlesToOutcome() {
        Payout p = Payout.accepted("P-1", "ACC-001", BigDecimal.TEN, "BR1", PayoutStatus.FAILED, "ev");
        assertThat(p.getStatus()).isEqualTo(PayoutStatus.IN_PROGRESS);
        assertThat(p.isInProgress()).isTrue();

        LocalDateTime now = LocalDateTime.now();
        p.settle(now);

        assertThat(p.getStatus()).isEqualTo(PayoutStatus.FAILED);
        assertThat(p.getSettledAt()).isEqualTo(now);
        assertThat(p.getCallbackSentAt()).isEqualTo(now);
        assertThat(p.isInProgress()).isFalse();
    }

    @Test
    void cannotSettleTwice() {
        Payout p = Payout.accepted("P-1", "ACC-001", BigDecimal.TEN, "BR1", PayoutStatus.SUCCEEDED, "ev");
        p.settle(LocalDateTime.now());

        assertThatThrownBy(() -> p.settle(LocalDateTime.now())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void outcomeMustBeFinal() {
        assertThatThrownBy(() -> Payout.accepted("P-1", "ACC-001", BigDecimal.TEN, "BR1", PayoutStatus.IN_PROGRESS, "ev"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectedIsFinalImmediately() {
        Payout p = Payout.rejected("P-2", "XYZ", BigDecimal.TEN, PayoutRejectionReason.BENEFICIARY_ACCOUNT_INVALID);

        assertThat(p.getResult()).isEqualTo(PayoutResult.REJECTED);
        assertThat(p.getStatus()).isEqualTo(PayoutStatus.REJECTED);
        assertThat(p.getBankRef()).isNull();
        assertThatThrownBy(() -> p.settle(LocalDateTime.now())).isInstanceOf(IllegalStateException.class);
    }
}
