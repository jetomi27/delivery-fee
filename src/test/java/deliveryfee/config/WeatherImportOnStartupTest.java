package deliveryfee.config;

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
class WeatherImportOnStartupTest {

    @Mock
    private WeatherImportService weatherImportService;

    @InjectMocks
    private WeatherImportOnStartup weatherImportOnStartup;

    @Test
    void run_importsWeather() {
        weatherImportOnStartup.run(null);

        verify(weatherImportService).importObservations();
    }

    @Test
    void run_importFails_doesNotThrow() {
        when(weatherImportService.importObservations())
                .thenThrow(new WeatherDataFetchException("Service down", null));

        assertThatCode(() -> weatherImportOnStartup.run(null)).doesNotThrowAnyException();
    }
}