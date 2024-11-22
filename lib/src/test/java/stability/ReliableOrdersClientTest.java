package stability;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.workshop.stability.stubs.OrderStubs;
import stability.config.CircuitBreakerConfig;
import stability.exceptions.CircuitOpenException;
import stability.exceptions.TimeoutException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    public void fetchOrdersWithoutTimeoutThrowsExceptionIfOrdersEndpointIsSlow(WireMockRuntimeInfo wm) {
        // given
        ordersStubWithHighLatency(ORDERS_URI);  // Simulate slow endpoint
        long timeout = 100;  // Timeout in milliseconds
        long testTimeout = 500;  // Overall test timeout

        // when
        assertThrows(TimeoutException.class, () -> {
            withTimeout(() -> {
                ordersFetcher.fetchOrdersWithTimeout(Duration.ofMillis(timeout));
                return null; // we don't need to return anything from the fetch operation
            }, testTimeout, TimeUnit.MILLISECONDS);
        });

        // then
        verify(getRequestedFor(urlEqualTo(ORDERS_URI)));  // Ensure the endpoint was requested
    }

    @Test
    public void fetchOrdersWithTimeoutReturnTheListOfOrdersWhenEndpointLatencyIsLow() throws Exception {
        // given
        OrderStubs.ordersStub(ORDERS_URI, 3);

        // when
        List<Order> orders = ordersFetcher.fetchOrdersWithTimeout(Duration.ofMillis(100));

        // then
        assertEquals(3, orders.size());
        verify(getRequestedFor(urlEqualTo(ORDERS_URI)));
    }

    @Test
    public void fetchOrdersGetsListOfOrdersFromEndpointThatStartsWithErrors() throws Exception {
        // given
        OrderStubs.ordersStubWithErrors(ORDERS_URI, 4, 5);

        // when
        List<Order> orders = ordersFetcher.fetchOrdersWithTimeout(Duration.ofMillis(100));

        // then
        assertEquals(3, orders.size());
        verify(getRequestedFor(urlEqualTo(ORDERS_URI)), times(5));
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
        verify(getRequestedFor(urlEqualTo(ORDERS_URI)), times(5));
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
