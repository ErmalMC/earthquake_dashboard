package com.ermal.backend.dto;

import com.ermal.backend.model.Earthquake;
import java.time.Instant;

public record EarthquakeDTO(
        Long id,
        String usgsId,
        Double magnitude,
        String magType,
        String place,
        String title,
        Instant eventTime,
        Double latitude,
        Double longitude,
        Double depth,
        Instant fetchedAt
) {

    public static EarthquakeDTO fromEntity(Earthquake earthquake) {
        if (earthquake == null) {
            return null;
        }

        return new EarthquakeDTO(
                earthquake.getId(),
                earthquake.getUsgsId(),
                earthquake.getMagnitude(),
                earthquake.getMagType(),
                earthquake.getPlace(),
                earthquake.getTitle(),
                earthquake.getEventTime(),
                earthquake.getLatitude(),
                earthquake.getLongitude(),
                earthquake.getDepth(),
                earthquake.getFetchedAt()
        );
    }
}
