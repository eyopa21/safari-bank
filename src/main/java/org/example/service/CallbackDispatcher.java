package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.PayoutCallbackEvent;
import org.example.config.PayoutProperties;
import org.example.domain.Payout;
import org.example.domain.PayoutRepository;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Sends the outcome callback like the real bank: after a random delay, to a random
// instance, sometimes duplicated, retried when the client doesn't answer 2xx in time.
@Service
@Slf4j
@RequiredArgsConstructor
public class CallbackDispatcher {

    private final PayoutRepository payouts;
    private final PayoutProperties props;
    private final RestClient callbackRestClient;
    private final TaskScheduler callbackScheduler;
    private final TransactionTemplate tx;

    public void scheduleFor(Payout payout) {
        Runnable schedule = () -> {
            Duration delay = randomBetween(props.callback().delayMin(), props.callback().delayMax());
            log.info("payout {} decided {}, callback in {} ms", payout.getPayoutId(), payout.getOutcome(), delay.toMillis());
            callbackScheduler.schedule(() -> send(payout.getId()), Instant.now().plus(delay));
        };
        // don't fire before the payout row is committed
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule.run();
                }
            });
        } else {
            schedule.run();
        }
    }

    private void send(Long id) {
        PayoutCallbackEvent event = tx.execute(status -> {
            Payout p = payouts.findById(id).orElse(null);
            if (p == null || !p.isInProgress()) {
                return null;
            }
            p.settle(LocalDateTime.now());
            return new PayoutCallbackEvent(p.getEventId(), p.getPayoutId(), p.getBankRef(), p.getStatus());
        });
        if (event == null) {
            return;
        }
        int copies = ThreadLocalRandom.current().nextDouble() < props.callback().duplicateRate()
                ? ThreadLocalRandom.current().nextInt(2, 4) : 1;
        for (int i = 0; i < copies; i++) {
            String url = randomUrl();
            callbackScheduler.schedule(() -> deliver(event, url), Instant.now());
        }
    }

    private void deliver(PayoutCallbackEvent event, String url) {
        int maxAttempts = props.callback().maxRetries() + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var response = callbackRestClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(event)
                        .retrieve()
                        .toBodilessEntity();
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("callback {} ({}) delivered to {} on attempt {}", event.eventId(), event.status(), url, attempt);
                    return;
                }
                log.warn("callback {} to {} got {} (attempt {})", event.eventId(), url, response.getStatusCode(), attempt);
            } catch (Exception e) {
                log.warn("callback {} to {} failed: {} (attempt {})", event.eventId(), url, e.getMessage(), attempt);
            }
            sleep(props.callback().retryInterval());
        }
        log.error("callback {} to {} gave up after {} attempts", event.eventId(), url, maxAttempts);
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

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
