package stability;

import stability.config.CircuitBreakerConfig;
import stability.config.ExponentialBackoffConfig;

import java.time.Duration;
import java.util.List;

public class ReliableOrdersFetcher implements OrdersFetcher {
    private final String url;

    public ReliableOrdersFetcher(String url) {
        this.url = url;
    }

    @Override
    public List<Order> fetchOrders() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<Order> fetchOrdersWithFallback(List<Order> ordersFallback)
        throws InterruptedException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<Order> fetchOrdersWithRetries(int maxRetries, ExponentialBackoffConfig backoff) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<Order> fetchOrdersWithTimeout(Duration timeout) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<Order> fetchOrdersWithCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
