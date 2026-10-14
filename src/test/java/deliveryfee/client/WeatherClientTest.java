package deliveryfee.client;

import deliveryfee.dto.WeatherObservationsXml;
import deliveryfee.dto.WeatherStationXml;
import deliveryfee.exception.WeatherDataFetchException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class WeatherClientTest {
    private final WeatherClient client = new WeatherClient("");

    @Test
    void parse_readsTimestampAndAllStations() throws IOException {
        WeatherObservationsXml result = client.parse(readSample());

        assertThat(result.getTimestamp()).isEqualTo(1791978925L);
        assertThat(result.getStations()).hasSize(3);
    }

    @Test
    void parse_readsStationFields() throws IOException {
        WeatherObservationsXml result = client.parse(readSample());

        WeatherStationXml tallinn = result.getStations().get(1);
        assertThat(tallinn.getName()).isEqualTo("Tallinn-Harku");
        assertThat(tallinn.getWmoCode()).isEqualTo("26038");
        assertThat(tallinn.getAirTemperature()).isEqualTo("10.8");
        assertThat(tallinn.getWindSpeed()).isEqualTo("4.7");
        assertThat(tallinn.getPhenomenon()).isEqualTo("Cloudy with clear spells");
    }

    @Test
    void parse_emptyElementsAreEmpty() throws IOException {
        WeatherObservationsXml result = client.parse(readSample());

        WeatherStationXml kuressaare = result.getStations().get(0);
        assertThat(kuressaare.getWmoCode()).isNullOrEmpty();
        assertThat(kuressaare.getWindSpeed()).isNullOrEmpty();
        assertThat(kuressaare.getPhenomenon()).isNullOrEmpty();
    }

    @Test
    void parse_emptyResponse_throwsException() {
        assertThatThrownBy(() -> client.parse(""))
                .isInstanceOf(WeatherDataFetchException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void parse_invalidXml_throwsException() {
        assertThatThrownBy(() -> client.parse("<html>Service unavailable</html"))
                .isInstanceOf(WeatherDataFetchException.class)
                .hasMessageContaining("Failed to parse");
    }

    @Test
    void parse_validXmlWithoutTimestamp_throwsException() {
        assertThatThrownBy(() -> client.parse("<html>Service unavailable</html>"))
                .isInstanceOf(WeatherDataFetchException.class)
                .hasMessageContaining("no timestamp");
    }

    /**
    * Reads the sample observations XML from test resources.
    *
    * @return contents of observations-sample.xml as a UTF-8 string
    * @throws IOException if the file cannot be read
    * @throws IllegalStateException if the file is missing from test resources
    */
    private String readSample() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/observations-sample.xml")) {
            if (in == null) {
                throw new IllegalStateException("observations-sample.xml not found in test resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
}
