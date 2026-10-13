package deliveryfee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Root element of the observations XML: one timestamp and a list of stations.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonRootName("observations")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherObservationsXml {

    @JacksonXmlProperty(isAttribute = true)
    private Long timestamp;

    // stations are listed directly under <observations>, without a wrapping <stations> tag
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "station")
    private List<WeatherStationXml> stations = new ArrayList<>();
}
