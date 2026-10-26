package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "bank.payout")
public record PayoutProperties(
        String validAccountPattern,
        String failingAccountPattern,
        double failureRate,
        Duration statusLatencyMin,
        Duration statusLatencyMax,
        Callback callback) {

    public record Callback(
            List<String> urls,
            Duration delayMin,
            Duration delayMax,
            double duplicateRate,
            Duration responseTimeout,
            Duration retryInterval,
            int maxRetries) {
    }
}
