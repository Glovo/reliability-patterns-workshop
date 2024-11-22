package stability;

import java.io.IOException;
import java.net.http.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import stability.config.CircuitBreakerConfig;
import stability.config.ExponentialBackoffConfig;

public class ExampleOrdersClient implements OrdersFetcher {
    private final String uri;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ExampleOrdersClient(String uri) {
        this.uri = uri;
    }

    @Override
    public List<Order> fetchOrders() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse the JSON response to a List<Order>
        return objectMapper.readValue(response.body(), new TypeReference<List<Order>>() {});
    }

    @Override
    public List<Order> fetchOrdersWithFallback() {
        return List.of();
    }

    @Override
    public List<Order> fetchOrdersWithRetries(int maxRetries, ExponentialBackoffConfig backoffConfig) {
        return List.of();
    }

    @Override
    public List<Order> fetchOrdersWithTimeout(Duration timeout) {
        return List.of();
    }

    @Override
    public List<Order> fetchOrdersWithCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        return List.of();
    }
}
