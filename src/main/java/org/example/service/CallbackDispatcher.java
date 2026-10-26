package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.PayoutCallbackEvent;
import org.example.config.PayoutProperties;
import org.example.domain.CallbackOutbox;
import org.example.domain.CallbackOutboxRepository;
import org.example.domain.Payout;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Outbox: callback copies are rows written with the payout. A poller claims due rows,
// settles the payout, and posts them. Rows survive a restart, so no callback is lost.
@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackDispatcher {

    private final CallbackOutboxRepository outbox;
    private final PayoutProperties props;
    private final RestClient callbackRestClient;
    private final ThreadPoolTaskExecutor callbackExecutor;
    private final TransactionTemplate tx;

    // called inside the submit transaction, so payout and its callbacks commit together
    public void enqueue(Payout payout) {
        LocalDateTime sendAt = LocalDateTime.now().plus(randomBetween(props.callback().delayMin(), props.callback().delayMax()));
        int copies = ThreadLocalRandom.current().nextDouble() < props.callback().duplicateRate()
                ? ThreadLocalRandom.current().nextInt(2, 4) : 1;
        for (int i = 0; i < copies; i++) {
            outbox.save(new CallbackOutbox(payout, randomUrl(), sendAt));
        }
        log.info("payout {} decided {}, {} callback(s) at {}", payout.getPayoutId(), payout.getOutcome(), copies, sendAt);
    }

    @Scheduled(fixedDelayString = "${bank.payout.callback.poll-interval}")
    public void poll() {
        List<Delivery> due;
        try {
            due = claimDue();
        } catch (Exception e) {
            log.error("outbox poll failed: {}", e.getMessage());
            return;
        }
        for (Delivery d : due) {
            callbackExecutor.execute(() -> post(d));
        }
    }

    // one transaction: claim the rows and settle their payouts
    private List<Delivery> claimDue() {
        return tx.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            List<Delivery> claimed = new ArrayList<>();
            for (CallbackOutbox row : outbox.findDue(props.callback().maxRetries() + 1, 100)) {
                Payout p = row.getPayout();
                if (p.isInProgress()) {
                    p.settle(now);
                }
                row.attempt(now.plus(props.callback().retryInterval()));
                claimed.add(new Delivery(row.getId(), row.getUrl(), row.getAttempts(),
                        new PayoutCallbackEvent(p.getEventId(), p.getPayoutId(), p.getBankRef(), p.getStatus())));
            }
            return claimed;
        });
    }

    private void post(Delivery d) {
        int maxAttempts = props.callback().maxRetries() + 1;
        try {
            var response = callbackRestClient.post()
                    .uri(d.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(d.event())
                    .retrieve()
                    .toBodilessEntity();
            if (response.getStatusCode().is2xxSuccessful()) {
                markSent(d.id());
                log.info("callback {} ({}) delivered to {} on attempt {}", d.event().eventId(), d.event().status(), d.url(), d.attempt());
                return;
            }
            log.warn("callback {} to {} got {} (attempt {}/{})", d.event().eventId(), d.url(), response.getStatusCode(), d.attempt(), maxAttempts);
        } catch (Exception e) {
            log.warn("callback {} to {} failed: {} (attempt {}/{})", d.event().eventId(), d.url(), e.getMessage(), d.attempt(), maxAttempts);
        }
        if (d.attempt() >= maxAttempts) {
            log.error("callback {} to {} gave up after {} attempts", d.event().eventId(), d.url(), maxAttempts);
        }
    }

    private void markSent(Long id) {
        tx.executeWithoutResult(status -> outbox.findById(id).ifPresent(row -> row.sent(LocalDateTime.now())));
    }

    private String randomUrl() {
        List<String> urls = props.callback().urls();
        return urls.get(ThreadLocalRandom.current().nextInt(urls.size()));
    }

    static Duration randomBetween(Duration min, Duration max) {
        long lo = min.toMillis();
        long hi = Math.max(lo, max.toMillis());
        return Duration.ofMillis(lo == hi ? lo : ThreadLocalRandom.current().nextLong(lo, hi + 1));
    }

    record Delivery(Long id, String url, int attempt, PayoutCallbackEvent event) {
    }
}
