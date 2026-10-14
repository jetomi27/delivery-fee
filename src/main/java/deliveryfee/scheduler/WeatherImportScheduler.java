package deliveryfee.scheduler;

import deliveryfee.exception.WeatherDataFetchException;
import deliveryfee.service.WeatherImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the weather data import on a configurable schedule
 */
@Slf4j
@Component
public class WeatherImportScheduler {

    private final WeatherImportService weatherImportService;

    public WeatherImportScheduler(WeatherImportService weatherImportService) {
        this.weatherImportService = weatherImportService;
    }

    /**
     * Imports weather observations. Runs at HH:15:00 every hour by default
     */
    @Scheduled(cron = "${weather.import.cron}")
    public void importWeather() {
        try {
            weatherImportService.importObservations();
        } catch (WeatherDataFetchException e) {
            log.error("Weather import failed: {}", e.getMessage(), e);
        }
    }
}