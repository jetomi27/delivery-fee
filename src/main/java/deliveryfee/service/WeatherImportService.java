package deliveryfee.service;

import deliveryfee.client.WeatherClient;
import deliveryfee.dto.WeatherObservationsXml;
import deliveryfee.dto.WeatherStationXml;
import deliveryfee.entity.City;
import deliveryfee.entity.WeatherObservation;
import deliveryfee.repository.WeatherObservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports weather observations for supported cities and stores them
 */
@Slf4j
@Service
public class WeatherImportService {

    private final WeatherClient weatherClient;
    private final WeatherObservationRepository weatherObservationRepository;

    public WeatherImportService(WeatherClient weatherClient,
                            WeatherObservationRepository weatherObservationRepository) {
        this.weatherClient = weatherClient;
        this.weatherObservationRepository = weatherObservationRepository;
    }

    /**
     * Downloads current observations and saves new entries for the stations of supported cities.
     *
     * @return saved observations
     */
    @Transactional
    public List<WeatherObservation> importObservations() {
        WeatherObservationsXml data = weatherClient.fetchObservations();
        Instant observedAt = Instant.ofEpochSecond(data.getTimestamp());
        Set<String> requiredStations = City.stationNames();

        List<WeatherObservation> found = data.getStations().stream()
                .filter(station -> station.getName() != null)
                .filter(station -> requiredStations.contains(station.getName().trim()))
                .map(station -> toEntity(station, observedAt))
                .toList();

        if (found.size() < requiredStations.size()) {
            log.warn("Expected {} stations but found {} in weather data",
                    requiredStations.size(), found.size());
        }

        List<WeatherObservation> newObservations = found.stream()
            .filter(observation -> !weatherObservationRepository.existsByStationNameAndObservedAt(
                    observation.getStationName(), observation.getObservedAt()))
            .toList();

        if (newObservations.isEmpty()) {
            log.info("Weather data for {} is already imported, nothing to save", observedAt);
            return List.of();
        }

        List<WeatherObservation> saved = weatherObservationRepository.saveAll(newObservations);
        log.info("Imported {} weather observations observed at {}", saved.size(), observedAt);
        return saved;
    }

    private WeatherObservation toEntity(WeatherStationXml station, Instant observedAt) {
        return WeatherObservation.builder()
                .stationName(station.getName().trim())
                .wmoCode(emptyToNull(station.getWmoCode()))
                .airTemperature(parseDouble(station.getAirTemperature()))
                .windSpeed(parseDouble(station.getWindSpeed()))
                .phenomenon(emptyToNull(station.getPhenomenon()))
                .observedAt(observedAt)
                .build();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double parseDouble(String value) {
        String cleaned = emptyToNull(value);
        if (cleaned == null) {
            return null;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse numeric weather value '{}'", value);
            return null;
        }
    }
}