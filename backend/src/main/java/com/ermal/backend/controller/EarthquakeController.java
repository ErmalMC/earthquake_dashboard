package com.ermal.backend.controller;

import com.ermal.backend.dto.EarthquakeDTO;
import com.ermal.backend.service.EarthquakeService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/earthquakes")
public class EarthquakeController {

    private final EarthquakeService earthquakeService;

    public EarthquakeController(EarthquakeService earthquakeService) {
        this.earthquakeService = earthquakeService;
    }

    @GetMapping
    public ResponseEntity<List<EarthquakeDTO>> getStoredEarthquakes(
            @RequestParam(required = false) Double minMagnitude,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    ) {
        return ResponseEntity.ok(earthquakeService.getStoredEarthquakes(minMagnitude, startTime, endTime));
    }

    @PostMapping("/refresh")
    public ResponseEntity<List<EarthquakeDTO>> refreshEarthquakes() {
        return ResponseEntity.ok(earthquakeService.refreshEarthquakes());
    }

    @DeleteMapping("/{usgsId}")
    public ResponseEntity<Void> deleteByUsgsId(@PathVariable String usgsId) {
        boolean deleted = earthquakeService.deleteByUsgsId(usgsId);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
