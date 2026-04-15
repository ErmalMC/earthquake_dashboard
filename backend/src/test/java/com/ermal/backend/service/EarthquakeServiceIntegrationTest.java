package com.ermal.backend.service;

import com.ermal.backend.dto.EarthquakeDTO;
import com.ermal.backend.repository.EarthquakeRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EarthquakeServiceIntegrationTest {

    private static final HttpServer MOCK_USGS_SERVER;
    private static final int MOCK_USGS_PORT;
    private static final AtomicInteger MOCK_STATUS = new AtomicInteger(200);
    private static final AtomicInteger MOCK_DELAY_MS = new AtomicInteger(0);
    private static final AtomicReference<String> MOCK_BODY = new AtomicReference<>("{\"features\":[]}");

    static {
        try {
            MOCK_USGS_SERVER = HttpServer.create(new InetSocketAddress(0), 0);
            MOCK_USGS_SERVER.createContext("/all_hour.geojson", new MockUsgsHandler());
            MOCK_USGS_SERVER.start();
            MOCK_USGS_PORT = MOCK_USGS_SERVER.getAddress().getPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start mock USGS server for tests.", e);
        }
    }

    @Autowired
    private EarthquakeService earthquakeService;

    @Autowired
    private EarthquakeRepository earthquakeRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("earthquake.usgs.url", () -> "http://127.0.0.1:" + MOCK_USGS_PORT + "/all_hour.geojson");
        registry.add("earthquake.filter.min-magnitude", () -> "2.0");
        registry.add("earthquake.filter.since", () -> "1970-01-01T00:00:00Z");
        registry.add("earthquake.usgs.connect-timeout-ms", () -> "1000");
        registry.add("earthquake.usgs.request-timeout-ms", () -> "1000");
    }

    @AfterAll
    static void stopMockServer() {
        MOCK_USGS_SERVER.stop(0);
    }

    @BeforeEach
    void setUp() {
        earthquakeRepository.deleteAllInBatch();
        MOCK_STATUS.set(200);
        MOCK_DELAY_MS.set(0);
        MOCK_BODY.set(validPayload());
    }

    @Test
    void refreshEarthquakes_filtersInvalidAndLowMagnitudeRecords() {
        List<EarthquakeDTO> refreshed = earthquakeService.refreshEarthquakes();

        assertEquals(2, refreshed.size());
        assertEquals(2, earthquakeRepository.count());
        assertTrue(refreshed.stream().allMatch(item -> item.magnitude() != null && item.magnitude() > 2.0));
        assertTrue(refreshed.stream().allMatch(item -> item.eventTime() != null && item.eventTime().isAfter(Instant.EPOCH)));
    }

    @Test
    void refreshEarthquakes_replacesExistingDataInsteadOfAppending() {
        earthquakeService.refreshEarthquakes();
        assertEquals(2, earthquakeRepository.count());

        MOCK_BODY.set(singleRecordPayload());
        earthquakeService.refreshEarthquakes();

        assertEquals(1, earthquakeRepository.count());
        assertEquals(1, earthquakeService.getStoredEarthquakes().size());
    }

    @Test
    void refreshEarthquakes_throwsWhenUpstreamApiIsUnavailable() {
        MOCK_STATUS.set(503);
        MOCK_BODY.set("{\"error\":\"Service unavailable\"}");

        assertThrows(IllegalStateException.class, () -> earthquakeService.refreshEarthquakes());
    }

    @Test
    void refreshEarthquakes_throwsWhenUpstreamApiTimesOut() {
        MOCK_DELAY_MS.set(1600);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> earthquakeService.refreshEarthquakes());
        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    void getStoredEarthquakes_appliesMagnitudeAndTimeFilters() {
        earthquakeService.refreshEarthquakes();

        List<EarthquakeDTO> filtered = earthquakeService.getStoredEarthquakes(
                3.6,
                Instant.parse("2024-04-16T00:00:00Z"),
                Instant.parse("2024-04-16T23:59:59Z")
        );

        assertEquals(1, filtered.size());
        assertEquals("eq-3", filtered.getFirst().usgsId());
    }

    @Test
    void getStoredEarthquakes_throwsForInvalidTimeRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> earthquakeService.getStoredEarthquakes(
                        2.0,
                        Instant.parse("2026-04-15T13:00:00Z"),
                        Instant.parse("2026-04-15T12:00:00Z")
                )
        );
    }

    private static String validPayload() {
        return """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "id": "eq-1",
                      "properties": {
                        "mag": 3.5,
                        "magType": "mb",
                        "place": "10km SW of Test City",
                        "title": "M 3.5 - Test City",
                        "time": 1713240000000
                      },
                      "geometry": {
                        "coordinates": [21.43, 41.99, 12.1]
                      }
                    },
                    {
                      "id": "eq-2",
                      "properties": {
                        "mag": 2.0,
                        "magType": "ml",
                        "place": "Low magnitude",
                        "title": "M 2.0 - Should be filtered",
                        "time": 1713241000000
                      },
                      "geometry": {
                        "coordinates": [20.11, 41.11, 5.0]
                      }
                    },
                    {
                      "id": "eq-3",
                      "properties": {
                        "mag": 4.2,
                        "magType": "mw",
                        "place": "15km NE of Another City",
                        "title": "M 4.2 - Another City",
                        "time": 1713242000000
                      },
                      "geometry": {
                        "coordinates": [19.55, 40.88, 8.2]
                      }
                    },
                    {
                      "id": "eq-4",
                      "properties": {
                        "mag": null,
                        "magType": "mw",
                        "place": "Missing magnitude",
                        "title": "Should be ignored",
                        "time": 1713243000000
                      },
                      "geometry": {
                        "coordinates": [19.0, 40.0, 7.0]
                      }
                    }
                  ]
                }
                """;
    }

    private static String singleRecordPayload() {
        return """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "id": "eq-single",
                      "properties": {
                        "mag": 5.1,
                        "magType": "mw",
                        "place": "Single Result",
                        "title": "M 5.1 - Single Result",
                        "time": 1713244000000
                      },
                      "geometry": {
                        "coordinates": [22.0, 42.0, 10.0]
                      }
                    }
                  ]
                }
                """;
    }

    private static final class MockUsgsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int delayMs = MOCK_DELAY_MS.get();
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            byte[] payload = MOCK_BODY.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(MOCK_STATUS.get(), payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        }
    }
}

