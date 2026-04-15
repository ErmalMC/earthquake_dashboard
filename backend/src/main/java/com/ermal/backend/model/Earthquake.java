package com.ermal.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "earthquakes")
@Getter
@Setter
@NoArgsConstructor
public class Earthquake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "usgs_id", nullable = false, unique = true, length = 64)
    private String usgsId;

    @Column(name = "magnitude")
    private Double magnitude;

    @Column(name = "mag_type", length = 16)
    private String magType;

    @Column(name = "place")
    private String place;

    @Column(name = "title")
    private String title;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "depth")
    private Double depth;

    @Column(name = "fetched_at")
    private Instant fetchedAt;
}
