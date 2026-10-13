package deliveryfee.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;


/**
 * Single station entry from the Estonian Environment Agency observations XML.
 * Values are kept as raw strings because the feed often contains empty elements.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherStationXml {

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "wmocode")
    private String wmoCode;

    @JacksonXmlProperty(localName = "airtemperature")
    private String airTemperature;

    @JacksonXmlProperty(localName = "windspeed")
    private String windSpeed;

    @JacksonXmlProperty(localName = "phenomenon")
    private String phenomenon;
}
