package stability;

import stability.config.CircuitBreakerConfig;
import stability.config.ExponentialBackoffConfig;
import stability.exceptions.CircuitOpenException;
import stability.exceptions.TimeoutException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public interface OrdersFetcher {
    List<Order> fetchOrders() throws InterruptedException, IOException;
    List<Order> fetchOrdersWithFallback(List<Order> ordersFallback) throws InterruptedException;
    List<Order> fetchOrdersWithRetries(int maxRetries, ExponentialBackoffConfig backoffConfig) throws InterruptedException;
    List<Order> fetchOrdersWithTimeout(Duration timeout) throws TimeoutException, InterruptedException;
    List<Order> fetchOrdersWithCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) throws CircuitOpenException, InterruptedException;
}

