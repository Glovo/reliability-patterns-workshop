package org.workshop.stability.stubs;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static fixtures.TestHelpers.someValidOrders;

public class OrderStubs {

    // Jackson ObjectMapper instance
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void ordersStubWithHighLatency(String uri) {
        stubFor(
                get(urlEqualTo(uri))
                        .willReturn(
                                aResponse()
                                        .withFixedDelay((int) Duration.ofHours(1).toMillis())
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(encodeToString(someValidOrders(5)))
                                        .withStatus(200)
                        )
        );
    }

    public static void ordersStubWithErrors(String uri, int errorsCount, int ordersCount) {
        if (errorsCount < 2) {
            throw new IllegalArgumentException("errors must be >= 2");
        }

        String scenarioName = "retry scenario";

        // Initial state: STARTED
        stubFor(
                get(urlEqualTo(uri))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willSetStateTo("retry0")
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(500) // Internal Server Error
                        )
        );

        // Additional retries
        int additionalRetries = errorsCount - 1;
        for (int i = 0; i < additionalRetries; i++) {
            stubFor(
                    get(urlEqualTo(uri))
                            .inScenario(scenarioName)
                            .whenScenarioStateIs("retry" + i)
                            .willSetStateTo("retry" + (i + 1))
                            .willReturn(
                                    aResponse()
                                            .withHeader("Content-Type", "application/json")
                                            .withStatus(500) // Internal Server Error
                            )
            );
        }

        // Final successful response
        stubFor(
                get(urlEqualTo(uri))
                        .inScenario(scenarioName)
                        .whenScenarioStateIs("retry" + additionalRetries)
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(encodeToString(someValidOrders(ordersCount)))
                                        .withStatus(200) // OK
                        )
        );
    }

    public static void ordersStub(String uri, int numberOfOrders) {
        stubFor(
                get(urlEqualTo(uri))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(encodeToString(someValidOrders(numberOfOrders)))
                                        .withStatus(200)
                        )
        );
    }

    // Helper method for Jackson serialization
    private static String encodeToString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }
}
