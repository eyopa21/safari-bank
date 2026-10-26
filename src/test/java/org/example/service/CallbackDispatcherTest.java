package org.example.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackDispatcherTest {

    @Test
    void randomBetweenStaysInsideBounds() {
        for (int i = 0; i < 1000; i++) {
            Duration d = CallbackDispatcher.randomBetween(Duration.ofSeconds(1), Duration.ofSeconds(10));
            assertThat(d).isBetween(Duration.ofSeconds(1), Duration.ofSeconds(10));
        }
    }

    @Test
    void randomBetweenHandlesEqualAndInvertedBounds() {
        assertThat(CallbackDispatcher.randomBetween(Duration.ofSeconds(2), Duration.ofSeconds(2))).isEqualTo(Duration.ofSeconds(2));
        assertThat(CallbackDispatcher.randomBetween(Duration.ofSeconds(5), Duration.ofSeconds(1))).isEqualTo(Duration.ofSeconds(5));
    }
}
