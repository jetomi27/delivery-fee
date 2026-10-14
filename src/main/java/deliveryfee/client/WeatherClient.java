package deliveryfee.client;

import deliveryfee.dto.WeatherObservationsXml;
import deliveryfee.exception.WeatherDataFetchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Downloads weather observations 
 */
@Component
public class WeatherClient {

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final String url;

    public WeatherClient(@Value("${weather.api.url}") String url) {
        this.restClient = RestClient.create();
        this.xmlMapper = new XmlMapper();
        this.url = url;
    }

    /**
    * Downloads the latest observations for all stations.
    *
    * @return parsed observations
    * @throws WeatherDataFetchException if the request fails or the response cannot be parsed
    */
    public WeatherObservationsXml fetchObservations() {
        String xml;
        try {
            xml = restClient.get()
                    .uri(url)
                    .retrieve() // send a request and get a response
                    .body(String.class);
        } catch (RestClientException e) {
            throw new WeatherDataFetchException("Failed to download weather data from " + url, e);
        }
        return parse(xml);
    }

    WeatherObservationsXml parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new WeatherDataFetchException("Weather service returned an empty response", null);
        }
        
        WeatherObservationsXml result;
        try {
            result = xmlMapper.readValue(xml, WeatherObservationsXml.class);
        } catch (JacksonException e) {
            throw new WeatherDataFetchException("Failed to parse weather data XML", e);
        }

        if (result.getTimestamp() == null) {
            throw new WeatherDataFetchException("Weather data XML has no timestamp", null);
        }
        return result;
    }
}
