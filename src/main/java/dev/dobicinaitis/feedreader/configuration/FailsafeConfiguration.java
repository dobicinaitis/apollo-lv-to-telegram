package dev.dobicinaitis.feedreader.configuration;

import dev.failsafe.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class FailsafeConfiguration {

    public static final int MAX_RETRY_COUNT = 3;
    public static final int DELAY_IN_SECONDS = 1;
    public static final RetryPolicy<Object> RETRY_POLICY;

    private FailsafeConfiguration() {
        throw new IllegalStateException("Utility class");
    }

    static {
        RETRY_POLICY = RetryPolicy.builder()
                .withMaxRetries(MAX_RETRY_COUNT)
                .withDelay(Duration.ofSeconds(DELAY_IN_SECONDS))
                .onFailedAttempt(e -> log.error("Action failed, reason: {}", e.getLastException().getMessage()))
                .onRetry(e -> log.info("Retrying, attempt {} of " + MAX_RETRY_COUNT + ".", e.getAttemptCount()))
                .onAbort(e -> log.error("Final retry failed, reason: ", e.getException()))
                .build();
    }

}