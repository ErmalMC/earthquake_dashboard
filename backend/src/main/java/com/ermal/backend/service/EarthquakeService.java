package com.ermal.backend.service;

import com.ermal.backend.dto.EarthquakeDTO;
import com.ermal.backend.model.Earthquake;
import com.ermal.backend.repository.EarthquakeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EarthquakeService {

    private final EarthquakeRepository earthquakeRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final URI usgsUri;
    private final double minMagnitude;
    private final Instant sinceThreshold;

    public EarthquakeService(
            EarthquakeRepository earthquakeRepository,
            ObjectMapper objectMapper,
            @Value("${earthquake.usgs.url:https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson}") String usgsUrl,
            @Value("${earthquake.filter.min-magnitude:2.0}") double minMagnitude,
            @Value("${earthquake.filter.since:1970-01-01T00:00:00Z}") String sinceConfig
    ) {
        this.earthquakeRepository = earthquakeRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.usgsUri = URI.create(usgsUrl);
        this.minMagnitude = minMagnitude;
        this.sinceThreshold = parseSinceInstant(sinceConfig);
    }

    @Transactional
    public List<EarthquakeDTO> refreshEarthquakes() {
        List<Earthquake> parsedEarthquakes = fetchAndParseEarthquakes(Instant.now());

        earthquakeRepository.deleteAllInBatch();
        List<Earthquake> storedEarthquakes = earthquakeRepository.saveAll(parsedEarthquakes);

        return toSortedDtos(storedEarthquakes);
    }

    public List<EarthquakeDTO> getStoredEarthquakes() {
        return toSortedDtos(earthquakeRepository.findAllByOrderByEventTimeDesc());
    }

    @Transactional
    public boolean deleteByUsgsId(String usgsId) {
        if (usgsId == null || usgsId.isBlank()) {
            return false;
        }
        return earthquakeRepository.deleteByUsgsId(usgsId.trim()) > 0;
    }

    private List<Earthquake> fetchAndParseEarthquakes(Instant fetchedAt) {
        String responseBody = fetchUsgsPayload();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode features = root.path("features");
            if (!features.isArray()) {
                throw new IllegalStateException("USGS response is missing the features array.");
            }

            Map<String, Earthquake> deduplicatedByUsgsId = new LinkedHashMap<>();
            for (JsonNode feature : features) {
                Earthquake parsed = parseFeature(feature, fetchedAt);
                if (parsed != null) {
                    deduplicatedByUsgsId.put(parsed.getUsgsId(), parsed);
                }
            }

            return new ArrayList<>(deduplicatedByUsgsId.values());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse USGS earthquake response.", e);
        }
    }

    private String fetchUsgsPayload() {
        HttpRequest request = HttpRequest.newBuilder(usgsUri).GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        String.format(Locale.ROOT, "USGS API returned HTTP %d", response.statusCode())
                );
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call USGS API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("USGS API call was interrupted.", e);
        }
    }

    private Earthquake parseFeature(JsonNode feature, Instant fetchedAt) {
        String usgsId = asText(feature.path("id"));
        if (usgsId == null) {
            return null;
        }

        JsonNode properties = feature.path("properties");
        Double magnitude = asDouble(properties.path("mag"));
        Long timeMillis = asLong(properties.path("time"));

        if (magnitude == null || magnitude <= minMagnitude || timeMillis == null) {
            return null;
        }

        Instant eventTime = Instant.ofEpochMilli(timeMillis);
        if (!eventTime.isAfter(sinceThreshold)) {
            return null;
        }

        JsonNode coordinates = feature.path("geometry").path("coordinates");

        Earthquake earthquake = new Earthquake();
        earthquake.setUsgsId(usgsId);
        earthquake.setMagnitude(magnitude);
        earthquake.setMagType(asText(properties.path("magType")));
        earthquake.setPlace(asText(properties.path("place")));
        earthquake.setTitle(asText(properties.path("title")));
        earthquake.setEventTime(eventTime);
        earthquake.setLongitude(asCoordinate(coordinates, 0));
        earthquake.setLatitude(asCoordinate(coordinates, 1));
        earthquake.setDepth(asCoordinate(coordinates, 2));
        earthquake.setFetchedAt(fetchedAt);

        return earthquake;
    }

    private Instant parseSinceInstant(String sinceConfig) {
        try {
            return Instant.parse(sinceConfig);
        } catch (Exception ex) {
            return Instant.EPOCH;
        }
    }

    private List<EarthquakeDTO> toSortedDtos(List<Earthquake> earthquakes) {
        return earthquakes.stream()
                .sorted(Comparator.comparing(Earthquake::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(EarthquakeDTO::fromEntity)
                .toList();
    }

    private String asText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String value = node.asText();
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Double asDouble(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asLong();
    }

    private Double asCoordinate(JsonNode coordinates, int index) {
        if (coordinates == null || !coordinates.isArray() || coordinates.size() <= index) {
            return null;
        }
        return asDouble(coordinates.get(index));
    }
}
