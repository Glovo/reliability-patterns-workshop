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

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void fetchOrdersWithCircuitBreakerGetsListOfOrdersFromEndpointThatExpectsBackpressure() throws Exception {
        // given
        int ordersCount = 5;
        OrderStubs.ordersStubWithErrors(ORDERS_URI, 5, ordersCount);
        int fetchMethodCallsCounter = 0;
        List<Order> orders;

        // when
        while (true) {
            try {
                fetchMethodCallsCounter++;
                orders = ordersFetcher.fetchOrdersWithCircuitBreaker(new CircuitBreakerConfig(2, Duration.ofMillis(500)));
                break;
            } catch (Exception e) {
                // keep iterating
            }
        }

        // then
        assertEquals(ordersCount, orders.size());
        System.out.println(fetchMethodCallsCounter);
        assertTrue(fetchMethodCallsCounter > 10);
        verify(6, getRequestedFor(urlEqualTo(ORDERS_URI)));
    }

}