package stability;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.workshop.stability.stubs.OrderStubs;
import stability.config.CircuitBreakerConfig;
import stability.config.ExponentialBackoffConfig;
import stability.exceptions.CircuitOpenException;
import stability.exceptions.TimeoutException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.workshop.stability.stubs.OrderStubs.ordersStubWithHighLatency;

@WireMockTest
public class ReliableOrdersClientTest {

    private static final String ORDERS_URI = "/orders";
    private OrdersFetcher ordersFetcher;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wm) {
        ordersFetcher = new ReliableOrdersFetcher("http://127.0.0.1:" + wm.getHttpPort() + ORDERS_URI);
    }

    @Test
    public void fetchOrdersReturnTheListOfOrders() throws Exception {
        // given
        OrderStubs.ordersStub(ORDERS_URI, 3);

        // when
        List<Order> orders = ordersFetcher.fetchOrders();

        // then
        assertEquals(3, orders.size());
        verify(getRequestedFor(urlEqualTo(ORDERS_URI)));
    }

    @Test
    public void fetchOrdersWithFallbackReturnTheListOfOrders() throws Exception {
        // given
        OrderStubs.ordersStubErrorFallback(ORDERS_URI);

        // when
        List<Order> ordersFallback =  asList(new Order(1, Collections.emptyList(), 3), new Order(2, Collections.emptyList(), 3));
        List<Order> orders = ordersFetcher.fetchOrdersWithFallback(ordersFallback);

        // then
        assertEquals(2, orders.size());
        verify(getRequestedFor(urlEqualTo(ORDERS_URI)));
    }

    @Test
    public void fetchOrdersWithRetriesReturnsTheListOfOrdersAfterXretries() throws Exception {
        // given
        OrderStubs.ordersStubWithErrors(ORDERS_URI, 4, 2);

        // when
        List<Order> orders = ordersFetcher.fetchOrdersWithRetries(5, new ExponentialBackoffConfig(Duration.ofMillis(100), 2, Duration.ofSeconds(1)));

        // then
        assertEquals(2, orders.size());
        verify(5, getRequestedFor(urlEqualTo(ORDERS_URI)));
    }

    @Nested
    class TimeoutTest {

        @Test
        public void fetchOrdersWithTimeoutReturnTheListOfOrdersWhenEndpointLatencyIsLow() throws Exception {
            // given
            OrderStubs.ordersStub(ORDERS_URI, 3);

            // when
            Duration maximumTimeToWaitbeforeTimeout = Duration.ofSeconds(1);
            List<Order> orders = ordersFetcher.fetchOrdersWithTimeout(maximumTimeToWaitbeforeTimeout);

            // then
            assertEquals(3, orders.size());
            verify(getRequestedFor(urlEqualTo(ORDERS_URI)));
        }

        @Test
        public void fetchOrdersGetsListOfOrdersFromEndpointThatStartsWithErrors() throws Exception {
            // given
            OrderStubs.ordersStubWithHighLatency(ORDERS_URI);

            // when
            assertThrows(TimeoutException.class, () -> ordersFetcher.fetchOrdersWithTimeout(Duration.ofSeconds(1)));

            // then
            verify(getRequestedFor(urlEqualTo(ORDERS_URI)));
        }
    }

    @Test
    public void fetchOrdersWithCircuitBreakerGetsListOfOrdersFromEndpointThatExpectsBackpressure(WireMockRuntimeInfo wm) throws Exception {
        // given
        int ordersCount = 5;
        OrderStubs.ordersStubWithErrors(ORDERS_URI, 5, ordersCount);
        int fetchMethodCallsCounter = 0;
        List<Order> orders;

        // when
        while (true) {
            try {
                fetchMethodCallsCounter++;
                orders = ordersFetcher.fetchOrdersWithCircuitBreaker(new CircuitBreakerConfig(5, Duration.ofMillis(100)));
                break;
            } catch (CircuitOpenException e) {
                // keep iterating
            }
        }

        // then
        assertEquals(ordersCount, orders.size());
        assertTrue(fetchMethodCallsCounter > 10);
        verify(5, getRequestedFor(urlEqualTo(ORDERS_URI)));
    }

    public static <T> T withTimeout(Callable<T> task, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);

        try {
            return future.get(timeout, unit);  // Wait for the task with a timeout
        } finally {
            executor.shutdownNow();  // Ensure the executor is shut down
        }
    }
}