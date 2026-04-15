package com.ermal.backend.controller;

import com.ermal.backend.dto.EarthquakeDTO;
import com.ermal.backend.service.EarthquakeService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EarthquakeController.class)
class EarthquakeControllerTest {

    private static final String BASE_PATH = "/api/earthquakes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EarthquakeService earthquakeService;

    private static EarthquakeDTO sampleDto(
            Long id,
            String usgsId,
            double magnitude,
            String place,
            Instant eventTime,
            Instant fetchedAt
    ) {
        return new EarthquakeDTO(
                id,
                usgsId,
                magnitude,
                "mw",
                place,
                "M " + magnitude + " - " + place,
                eventTime,
                41.0,
                21.0,
                10.0,
                fetchedAt
        );
    }

    @Test
    void getStoredEarthquakes_returnsOkAndList() throws Exception {
        when(earthquakeService.getStoredEarthquakes(null, null, null)).thenReturn(List.of(sampleDto(
                1L,
                "eq-1",
                3.4,
                "Test place",
                Instant.parse("2026-04-15T10:15:30Z"),
                Instant.parse("2026-04-15T10:20:30Z")
        )));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usgsId").value("eq-1"))
                .andExpect(jsonPath("$[0].magnitude").value(3.4))
                .andExpect(jsonPath("$[0].place").value("Test place"));
    }

    @Test
    void getStoredEarthquakes_withFilters_returnsOkAndList() throws Exception {
        Instant start = Instant.parse("2026-04-15T10:00:00Z");
        Instant end = Instant.parse("2026-04-15T12:00:00Z");

        when(earthquakeService.getStoredEarthquakes(3.0, start, end)).thenReturn(List.of(sampleDto(
                3L,
                "eq-filtered",
                4.1,
                "Filtered place",
                Instant.parse("2026-04-15T11:15:30Z"),
                Instant.parse("2026-04-15T11:20:30Z")
        )));

        mockMvc.perform(get(BASE_PATH)
                        .param("minMagnitude", "3.0")
                        .param("startTime", "2026-04-15T10:00:00Z")
                        .param("endTime", "2026-04-15T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usgsId").value("eq-filtered"))
                .andExpect(jsonPath("$[0].magnitude").value(4.1));
    }

    @Test
    void refreshEarthquakes_returnsOkAndList() throws Exception {
        when(earthquakeService.refreshEarthquakes()).thenReturn(List.of(sampleDto(
                2L,
                "eq-2",
                4.6,
                "Another place",
                Instant.parse("2026-04-15T11:15:30Z"),
                Instant.parse("2026-04-15T11:20:30Z")
        )));

        mockMvc.perform(post(BASE_PATH + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usgsId").value("eq-2"))
                .andExpect(jsonPath("$[0].magnitude").value(4.6));
    }

    @Test
    void deleteByUsgsId_returnsNoContentWhenDeleted() throws Exception {
        when(earthquakeService.deleteByUsgsId("eq-3")).thenReturn(true);

        mockMvc.perform(delete(BASE_PATH + "/eq-3"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteByUsgsId_returnsNotFoundWhenMissing() throws Exception {
        when(earthquakeService.deleteByUsgsId("missing-id")).thenReturn(false);

        mockMvc.perform(delete(BASE_PATH + "/missing-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStoredEarthquakes_returnsErrorBodyWhenServiceFails() throws Exception {
        doThrow(new IllegalStateException("USGS unavailable"))
                .when(earthquakeService)
                .getStoredEarthquakes(isNull(), isNull(), isNull());

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("USGS unavailable"))
                .andExpect(jsonPath("$.path").value(BASE_PATH));
    }

    @Test
    void getStoredEarthquakes_returnsBadRequestWhenFilterCombinationIsInvalid() throws Exception {
        doThrow(new IllegalArgumentException("startTime must be before or equal to endTime."))
                .when(earthquakeService)
                .getStoredEarthquakes(eq(2.0), eq(Instant.parse("2026-04-15T13:00:00Z")), eq(Instant.parse("2026-04-15T12:00:00Z")));

        mockMvc.perform(get(BASE_PATH)
                        .param("minMagnitude", "2.0")
                        .param("startTime", "2026-04-15T13:00:00Z")
                        .param("endTime", "2026-04-15T12:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTime must be before or equal to endTime."));
    }

    @Test
    void getStoredEarthquakes_returnsBadRequestWhenStartTimeFormatIsInvalid() throws Exception {
        mockMvc.perform(get(BASE_PATH)
                        .param("startTime", "not-an-instant"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'not-an-instant' for parameter 'startTime'."));
    }

    @Test
    void getStoredEarthquakes_returnsBadRequestWhenMinMagnitudeFormatIsInvalid() throws Exception {
        mockMvc.perform(get(BASE_PATH)
                        .param("minMagnitude", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'minMagnitude'."));
    }

    @Test
    void deleteByUsgsId_returnsBadRequestWhenInputIsInvalid() throws Exception {
        doThrow(new IllegalArgumentException("Invalid USGS id"))
                .when(earthquakeService)
                .deleteByUsgsId(anyString());

        mockMvc.perform(delete(BASE_PATH + "/ "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid USGS id"));
    }
}


