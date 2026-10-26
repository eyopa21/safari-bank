package org.example.service;

import org.example.config.PayoutProperties;
import org.example.domain.Payout;
import org.example.domain.PayoutRejectionReason;
import org.example.domain.PayoutRepository;
import org.example.domain.PayoutResult;
import org.example.domain.PayoutStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayoutServiceTest {

    PayoutRepository repo = mock(PayoutRepository.class);
    CallbackDispatcher callbacks = mock(CallbackDispatcher.class);
    PayoutService service;

    @BeforeEach
    void setUp() {
        PayoutProperties props = new PayoutProperties("ACC-\\d{3,}", "ACC-9\\d{2,}", 0.0, Duration.ZERO, Duration.ZERO,
                new PayoutProperties.Callback(List.of("http://x"), Duration.ZERO, Duration.ZERO, 0.0,
                        Duration.ofSeconds(2), Duration.ofSeconds(3), 5, Duration.ofMillis(500)));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new PayoutService(repo, props, callbacks);
    }

    @Test
    void acceptsValidAccountAndQueuesCallback() {
        Payout p = service.process("P-1", "ACC-001", new BigDecimal("5000"));

        assertThat(p.getResult()).isEqualTo(PayoutResult.ACCEPTED);
        assertThat(p.getBankRef()).matches("BR[0-9A-F]{16}");
        assertThat(p.getStatus()).isEqualTo(PayoutStatus.IN_PROGRESS);
        assertThat(p.getOutcome()).isEqualTo(PayoutStatus.SUCCEEDED);
        assertThat(p.getEventId()).isNotBlank();
        verify(callbacks).enqueue(p);
    }

    @Test
    void rejectsUnknownAccount() {
        Payout p = service.process("P-2", "XYZ", new BigDecimal("10"));

        assertThat(p.getResult()).isEqualTo(PayoutResult.REJECTED);
        assertThat(p.getReason()).isEqualTo(PayoutRejectionReason.BENEFICIARY_ACCOUNT_INVALID);
        assertThat(p.getStatus()).isEqualTo(PayoutStatus.REJECTED);
        verify(callbacks, never()).enqueue(any());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThat(service.process("P-3", "ACC-001", BigDecimal.ZERO).getReason()).isEqualTo(PayoutRejectionReason.INVALID_AMOUNT);
        assertThat(service.process("P-4", "ACC-001", new BigDecimal("-1")).getReason()).isEqualTo(PayoutRejectionReason.INVALID_AMOUNT);
        assertThat(service.process("P-5", "ACC-001", null).getReason()).isEqualTo(PayoutRejectionReason.INVALID_AMOUNT);
    }

    @Test
    void failingAccountPatternDecidesFailed() {
        Payout p = service.process("P-6", "ACC-901", new BigDecimal("10"));

        assertThat(p.getResult()).isEqualTo(PayoutResult.ACCEPTED);
        assertThat(p.getOutcome()).isEqualTo(PayoutStatus.FAILED);
    }

    @Test
    void repeatedPayoutIdReturnsStoredOutcome() {
        Payout stored = Payout.accepted("P-7", "ACC-001", BigDecimal.TEN, "BRAAAAAAAAAAAAAAAA", PayoutStatus.SUCCEEDED, "ev");
        when(repo.findByPayoutId("P-7")).thenReturn(Optional.of(stored));

        Payout p = service.process("P-7", "ACC-001", new BigDecimal("999"));

        assertThat(p).isSameAs(stored);
        verify(repo, never()).save(any());
        verify(callbacks, never()).enqueue(any());
    }

    @Test
    void statusOfUnknownPayoutIsNotFound() {
        when(repo.findByPayoutId("nope")).thenReturn(Optional.empty());

        assertThat(service.status("nope")).isEqualTo(PayoutStatus.NOT_FOUND);
    }

    @Test
    void statusReflectsStoredPayout() {
        Payout stored = Payout.rejected("P-8", "XYZ", BigDecimal.TEN, PayoutRejectionReason.BENEFICIARY_ACCOUNT_INVALID);
        when(repo.findByPayoutId("P-8")).thenReturn(Optional.of(stored));

        assertThat(service.status("P-8")).isEqualTo(PayoutStatus.REJECTED);
    }
}
