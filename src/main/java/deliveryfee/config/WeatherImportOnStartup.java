package deliveryfee.config;

import deliveryfee.exception.WeatherDataFetchException;
import deliveryfee.service.WeatherImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Imports weather data once on application startup, so fresh data is available
 * without waiting for the first scheduled run.
 * Can be turned off with {@code weather.import.on-startup=false}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "weather.import.on-startup", havingValue = "true", matchIfMissing = true)
public class WeatherImportOnStartup implements ApplicationRunner {

    private final WeatherImportService weatherImportService;

    public WeatherImportOnStartup(WeatherImportService weatherImportService) {
        this.weatherImportService = weatherImportService;
    }

    /**
     * Runs the weather import. A failed import is logged and does not stop the application.
     *
     * @param args application arguments, not used
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            weatherImportService.importObservations();
        } catch (WeatherDataFetchException e) {
            log.error("Weather import on startup failed: {}", e.getMessage(), e);
        }
    }
}