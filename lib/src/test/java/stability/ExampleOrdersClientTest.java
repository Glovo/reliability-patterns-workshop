package stability;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.workshop.stability.stubs.OrderStubs.*;

@WireMockTest
/**
 * You should not modify this test class. Implement every method in [ReliableOrdersFetcher] and run the tests
 * to see if your fetcher is reliable enough!
 */
public class ExampleOrdersClientTest {

    private static final String ORDERS_URI = "/orders";

    @BeforeEach
    void setup(WireMockRuntimeInfo wm) {
        // Initialization if needed
    }

    @Test
    public void fetchOrders_returnListOfOrders(WireMockRuntimeInfo wm) throws Exception {
        ordersStub(ORDERS_URI, 5);

        int port = wm.getHttpPort();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + ORDERS_URI))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        assertEquals(200, response.statusCode());
    }

    @Test
    public void fetchOrders_throwsExceptionIfOrdersEndpointIsSlow(WireMockRuntimeInfo wm) {
        ordersStubWithHighLatency(ORDERS_URI);

        int port = wm.getHttpPort();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();

        assertThrows(TimeoutException.class, () -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + ORDERS_URI))
                    .GET()
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        });
    }

    @Test
    public void fetchOrders_getListOfOrdersFromEndpointThatStartsWithErrors(WireMockRuntimeInfo wm) throws Exception {
        ordersStubWithErrors(ORDERS_URI, 5, 5);

        int port = wm.getHttpPort();
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 0; i < 5; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + ORDERS_URI))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + ORDERS_URI))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    public void fetchOrders_getListOfOrdersFromEndpointThatExpectsBackpressure(WireMockRuntimeInfo wm) throws Exception {
        ordersStubWithErrors(ORDERS_URI, 5, 5);

        int port = wm.getHttpPort();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + ORDERS_URI))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }
}
