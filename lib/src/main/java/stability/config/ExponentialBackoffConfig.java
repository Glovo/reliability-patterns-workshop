package stability.config;

import java.time.Duration;

public class ExponentialBackoffConfig {
    private final Duration initialDelay; // period of time to wait before the first retry attempt is sent
    private final Double factor; // multiplying factor to the initialDelay to avoid sending retries too fast
    private final Duration maxDelay;

    public ExponentialBackoffConfig(Duration initialDelay, double factor, Duration maxDelay) {
        this.initialDelay = initialDelay;
        this.factor = factor;
        this.maxDelay = maxDelay;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Double getFactor() {
        return factor;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }
}
