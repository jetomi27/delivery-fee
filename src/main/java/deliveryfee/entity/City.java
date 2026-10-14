package deliveryfee.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Cities supported by the delivery service and their weather stations
 */
@Getter
public enum City {
    TALLINN("Tallinn-Harku"),
    TARTU("Tartu-Tõravere"),
    PARNU("Pärnu");

    private final String stationName;

    City(String stationName) {
        this.stationName = stationName;
    }

    /**
     * Returns names of all weather stations that must be imported.
     *
     * @return station names of all supported cities
     */
    public static Set<String> stationNames() {
        return Arrays.stream(values())
                .map(City::getStationName)
                .collect(Collectors.toSet());
    }
}