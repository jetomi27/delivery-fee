package deliveryfee;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.entity.WeatherObservation;
import deliveryfee.repository.BaseFeeRepository;
import deliveryfee.repository.WeatherObservationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the whole application with an in-memory database and the default fees and rules,
 * and checks complete scenarios through the REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeliveryFeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeatherObservationRepository weatherObservationRepository;

    @Autowired
    private BaseFeeRepository baseFeeRepository;

    @Test
    void exampleFromTask_tartuBikeInLightSnow_costs4Euros() throws Exception {
        saveWeather(City.TARTU, -2.1, 4.7, "Light snow shower", Instant.now().minus(10, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/delivery-fee").param("city", "TARTU").param("vehicleType", "BIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(4.00))
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    void strongWind_bikeIsForbidden() throws Exception {
        saveWeather(City.PARNU, 5.0, 25.0, "Overcast", Instant.now().minus(10, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/delivery-fee").param("city", "PARNU").param("vehicleType", "BIKE"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Usage of selected vehicle type is forbidden"));
    }

    @Test
    void changedBaseFee_newFeeNowAndOldFeeForEarlierMoment() throws Exception {
        saveWeather(City.TARTU, 5.0, 2.0, "Overcast", Instant.now().minus(10, ChronoUnit.MINUTES));
        Instant beforeChange = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        Long tartuBikeFeeId = baseFeeRepository.findValidAt(City.TARTU, VehicleType.BIKE, Instant.now())
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/base-fees/" + tartuBikeFeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fee": 3.20}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/delivery-fee").param("city", "TARTU").param("vehicleType", "BIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(3.20));

        mockMvc.perform(get("/api/delivery-fee")
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", beforeChange.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(2.50));
    }

    private void saveWeather(City city, Double temperature, Double windSpeed, String phenomenon,
                             Instant observedAt) {
        weatherObservationRepository.save(WeatherObservation.builder()
                .stationName(city.getStationName())
                .airTemperature(temperature)
                .windSpeed(windSpeed)
                .phenomenon(phenomenon)
                .observedAt(observedAt)
                .build());
    }
}