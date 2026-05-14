package com.ermal.backend.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "earthquakes")
@Getter
@Setter
@NoArgsConstructor
public class Earthquake {

    @Id
    @Setter(AccessLevel.NONE)
    private String id;

    @Indexed(unique = true)
    private String usgsId;

    private Double magnitude;

    private String magType;

    private String place;

    private String title;

    private Instant eventTime;

    private Double latitude;

    private Double longitude;

    private Double depth;

    private Instant fetchedAt;
}
