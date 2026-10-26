package org.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payout_id", nullable = false, unique = true, length = 64)
    private String payoutId;

    @Column(name = "bank_account", nullable = false, length = 34)
    private String bankAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutResult result;

    @Column(name = "bank_ref", length = 32)
    private String bankRef;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private PayoutRejectionReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutStatus status;

    // decided at submit time, becomes the status once the callback goes out
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PayoutStatus outcome;

    @Column(name = "event_id", unique = true, length = 64)
    private String eventId;

    @Column(name = "callback_sent_at")
    private LocalDateTime callbackSentAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Payout(String payoutId, String bankAccount, BigDecimal amount) {
        this.payoutId = payoutId;
        this.bankAccount = bankAccount;
        this.amount = amount;
    }

    public static Payout accepted(String payoutId, String bankAccount, BigDecimal amount,
                                  String bankRef, PayoutStatus outcome, String eventId) {
        Payout p = new Payout(payoutId, bankAccount, amount);
        p.result = PayoutResult.ACCEPTED;
        p.bankRef = bankRef;
        p.status = PayoutStatus.IN_PROGRESS;
        p.outcome = outcome;
        p.eventId = eventId;
        return p;
    }

    public static Payout rejected(String payoutId, String bankAccount, BigDecimal amount, PayoutRejectionReason reason) {
        Payout p = new Payout(payoutId, bankAccount, amount);
        p.result = PayoutResult.REJECTED;
        p.reason = reason;
        p.status = PayoutStatus.REJECTED;
        return p;
    }

    public boolean isInProgress() {
        return status == PayoutStatus.IN_PROGRESS;
    }

    public void settle(LocalDateTime at) {
        if (!isInProgress()) {
            throw new IllegalStateException("payout " + payoutId + " is already " + status);
        }
        status = outcome;
        settledAt = at;
        callbackSentAt = at;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
