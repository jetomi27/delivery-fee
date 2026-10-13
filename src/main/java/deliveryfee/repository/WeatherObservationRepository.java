package deliveryfee.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import deliveryfee.entity.WeatherObservation;

public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {
    /** Returns the most recent observation for the given station. */
    Optional<WeatherObservation> findFirstByStationNameOrderByObservedAtDesc(String stationName);

    /** Returns the most recent observation for the given station at or before the given time. */
    Optional<WeatherObservation> findFirstByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
            String stationName, Instant time);
}
