package deliveryfee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import deliveryfee.client.WeatherClient;
import deliveryfee.dto.WeatherObservationsXml;
import deliveryfee.dto.WeatherStationXml;
import deliveryfee.entity.WeatherObservation;
import deliveryfee.repository.WeatherObservationRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherImportServiceTest {

    private static final long TIMESTAMP = 1791978925L;

    @Mock
    private WeatherClient weatherClient;

    @Mock
    private WeatherObservationRepository weatherObservationRepository;

    @InjectMocks
    private WeatherImportService weatherImportService;

    @Captor
    private ArgumentCaptor<List<WeatherObservation>> savedCaptor;

    @Test
    void importObservations_savesOnlySupportedStations() {
        givenStations(
                station("Kuressaare linn", "", "9.8", "", ""),
                station("Tallinn-Harku", "26038", "10.8", "4.7", "Overcast"),
                station("Tartu-Tõravere", "26242", "10.5", "2.7", "Clear"),
                station("Pärnu", "41803", "9.6", "10.1", "Light rain"));
        returnSavedObservations();

        weatherImportService.importObservations();

        verify(weatherObservationRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .extracting(WeatherObservation::getStationName)
                .containsExactlyInAnyOrder("Tallinn-Harku", "Tartu-Tõravere", "Pärnu");
    }

    @Test
    void importObservations_convertsRawValues() {
        givenStations(station("  Pärnu  ", "41803", " 9.6 ", "", ""));
        returnSavedObservations();

        weatherImportService.importObservations();

        verify(weatherObservationRepository).saveAll(savedCaptor.capture());
        WeatherObservation parnu = savedCaptor.getValue().get(0);
        assertThat(parnu.getStationName()).isEqualTo("Pärnu");
        assertThat(parnu.getAirTemperature()).isEqualTo(9.6);
        assertThat(parnu.getWindSpeed()).isNull();
        assertThat(parnu.getPhenomenon()).isNull();
        assertThat(parnu.getObservedAt()).isEqualTo(Instant.ofEpochSecond(TIMESTAMP));
    }

    @Test
    void importObservations_invalidNumber_savesNull() {
        givenStations(station("Pärnu", "41803", "N/A", "3.0", "Clear"));
        returnSavedObservations();

        weatherImportService.importObservations();

        verify(weatherObservationRepository).saveAll(savedCaptor.capture());
        WeatherObservation parnu = savedCaptor.getValue().get(0);
        assertThat(parnu.getAirTemperature()).isNull();
        assertThat(parnu.getWindSpeed()).isEqualTo(3.0);
    }

    @Test
    void importObservations_skipsAlreadyImportedObservations() {
        givenStations(
                station("Tallinn-Harku", "26038", "10.8", "4.7", "Overcast"),
                station("Pärnu", "41803", "9.6", "10.1", "Light rain"));
        when(weatherObservationRepository.existsByStationNameAndObservedAt(
                eq("Tallinn-Harku"), eq(Instant.ofEpochSecond(TIMESTAMP))))
                .thenReturn(true);
        returnSavedObservations();

        weatherImportService.importObservations();

        verify(weatherObservationRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .extracting(WeatherObservation::getStationName)
                .containsExactly("Pärnu");
    }

    @Test
    void importObservations_allAlreadyImported_savesNothing() {
        givenStations(station("Pärnu", "41803", "9.6", "10.1", "Light rain"));
        when(weatherObservationRepository.existsByStationNameAndObservedAt(
                eq("Pärnu"), eq(Instant.ofEpochSecond(TIMESTAMP))))
                .thenReturn(true);

        List<WeatherObservation> result = weatherImportService.importObservations();

        assertThat(result).isEmpty();
        verify(weatherObservationRepository, never()).saveAll(anyList());
    }

    private void givenStations(WeatherStationXml... stations) {
        WeatherObservationsXml data = new WeatherObservationsXml();
        data.setTimestamp(TIMESTAMP);
        data.setStations(List.of(stations));
        when(weatherClient.fetchObservations()).thenReturn(data);
    }

    private void returnSavedObservations() {
        when(weatherObservationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static WeatherStationXml station(String name, String wmoCode, String temperature,
                                             String windSpeed, String phenomenon) {
        WeatherStationXml station = new WeatherStationXml();
        station.setName(name);
        station.setWmoCode(wmoCode);
        station.setAirTemperature(temperature);
        station.setWindSpeed(windSpeed);
        station.setPhenomenon(phenomenon);
        return station;
    }
}