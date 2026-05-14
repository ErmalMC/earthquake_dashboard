package com.ermal.backend.repository;

import com.ermal.backend.model.Earthquake;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EarthquakeRepository extends MongoRepository<Earthquake, String> {

    Optional<Earthquake> findByUsgsId(String usgsId);

    boolean existsByUsgsId(String usgsId);


    long deleteByUsgsId(String usgsId);

    List<Earthquake> findAllByOrderByEventTimeDesc();

    List<Earthquake> findByMagnitudeGreaterThanOrderByEventTimeDesc(Double magnitude);

    List<Earthquake> findByEventTimeAfterOrderByEventTimeDesc(Instant eventTime);

    List<Earthquake> findByMagnitudeGreaterThanAndEventTimeAfterOrderByEventTimeDesc(Double magnitude, Instant eventTime);
}
