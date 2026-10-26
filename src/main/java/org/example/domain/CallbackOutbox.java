package org.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// one row per callback copy; sent_at null = still to deliver
@Entity
@Table(name = "callback_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallbackOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payout_id", nullable = false)
    private Payout payout;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CallbackOutbox(Payout payout, String url, LocalDateTime sendAt) {
        this.payout = payout;
        this.url = url;
        this.sendAt = sendAt;
        this.createdAt = LocalDateTime.now();
    }

    // claim: count the attempt and push the next try out, so a crash mid-send just retries later
    public void attempt(LocalDateTime nextTry) {
        attempts++;
        sendAt = nextTry;
    }

    public void sent(LocalDateTime at) {
        sentAt = at;
    }
}
