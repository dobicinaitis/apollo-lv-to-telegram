package dev.dobicinaitis.feedreader.configuration;

import dev.failsafe.Failsafe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static dev.dobicinaitis.feedreader.configuration.FailsafeConfiguration.DELAY_IN_SECONDS;
import static dev.dobicinaitis.feedreader.configuration.FailsafeConfiguration.RETRY_POLICY;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailsafeConfigurationTest {

    @Test
    @Timeout(5)
    void shouldFailAfterThreeAttemptsWithOneSecondDelays() {
        // given
        final var startTime = System.currentTimeMillis();
        // when
        try {
            Failsafe.with(RETRY_POLICY).run(() -> {
                throw new IllegalStateException("Intentional test exception");
            });
        } catch (IllegalStateException ignored) {
        }
        final var duration = System.currentTimeMillis() - startTime;
        // then
        assertTrue(duration >= DELAY_IN_SECONDS * 1000, "Should execute in at least 3 seconds");
    }

}