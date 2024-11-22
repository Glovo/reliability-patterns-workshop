package stability.config;

import java.time.Duration;

public class CircuitBreakerConfig {
    private final int failureThreshold; // number of errors before opening the circuit breaker
    private final Duration openTimeout; // period of time in open state. Once this period ends the state should move to half open.

    public CircuitBreakerConfig(int failureThreshold, Duration openTimeout) {
        this.failureThreshold = failureThreshold;
        this.openTimeout = openTimeout;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public Duration getOpenTimeout() {
        return openTimeout;
    }
}
