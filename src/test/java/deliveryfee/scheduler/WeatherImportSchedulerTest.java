package deliveryfee.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import deliveryfee.exception.WeatherDataFetchException;
import deliveryfee.service.WeatherImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherImportSchedulerTest {

    @Mock
    private WeatherImportService weatherImportService;

    @InjectMocks
    private WeatherImportScheduler weatherImportScheduler;

    @Test
    void importWeather_callsImportService() {
        weatherImportScheduler.importWeather();

        verify(weatherImportService).importObservations();
    }

    @Test
    void importWeather_fetchFails_doesNotThrow() {
        when(weatherImportService.importObservations())
                .thenThrow(new WeatherDataFetchException("Service down", null));

        assertThatCode(() -> weatherImportScheduler.importWeather())
                .doesNotThrowAnyException();
    }
}