package deliveryfee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import deliveryfee.dto.DeliveryFeeResponse;
import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import deliveryfee.entity.WeatherObservation;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.exception.VehicleUsageForbiddenException;
import deliveryfee.repository.BaseFeeRepository;
import deliveryfee.repository.ExtraFeeRuleRepository;
import deliveryfee.repository.WeatherObservationRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryFeeServiceTest {

    private static final Instant TIME = Instant.parse("2026-10-14T12:00:00Z");

    @Mock
    private BaseFeeRepository baseFeeRepository;

    @Mock
    private WeatherObservationRepository weatherObservationRepository;

    @Mock
    private ExtraFeeRuleRepository extraFeeRuleRepository;

    @InjectMocks
    private DeliveryFeeService deliveryFeeService;

    @Test
    void calculate_exampleFromTask_returns4Euros() {
        givenBaseFee(City.TARTU, VehicleType.BIKE, "2.50");
        givenWeather(City.TARTU, -2.1, 4.7, "Light snow shower");
        givenRules(VehicleType.BIKE, bikeRules());

        DeliveryFeeResponse result = deliveryFeeService.calculate(City.TARTU, VehicleType.BIKE, TIME);

        assertThat(result.fee()).isEqualByComparingTo("4.00");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void calculate_carHasNoExtraFees_returnsBaseFee() {
        givenBaseFee(City.TALLINN, VehicleType.CAR, "4.00");
        givenWeather(City.TALLINN, -15.0, 25.0, "Heavy snowfall");
        givenRules(VehicleType.CAR, List.of());

        DeliveryFeeResponse result = deliveryFeeService.calculate(City.TALLINN, VehicleType.CAR, TIME);

        assertThat(result.fee()).isEqualByComparingTo("4.00");
    }

    @Test
    void calculate_strongWindForBike_throwsForbidden() {
        givenBaseFee(City.PARNU, VehicleType.BIKE, "2.00");
        givenWeather(City.PARNU, 5.0, 25.0, "Overcast");
        givenRules(VehicleType.BIKE, bikeRules());

        assertThatThrownBy(() -> deliveryFeeService.calculate(City.PARNU, VehicleType.BIKE, TIME))
                .isInstanceOf(VehicleUsageForbiddenException.class)
                .hasMessage("Usage of selected vehicle type is forbidden");
    }

    @Test
    void calculate_glazeForScooter_throwsForbidden() {
        givenBaseFee(City.TARTU, VehicleType.SCOOTER, "3.00");
        givenWeather(City.TARTU, -1.0, 2.0, "Glaze");
        givenRules(VehicleType.SCOOTER, List.of(
                phenomenonRule(VehicleType.SCOOTER, PhenomenonCategory.GLAZE, null, true)));

        assertThatThrownBy(() -> deliveryFeeService.calculate(City.TARTU, VehicleType.SCOOTER, TIME))
                .isInstanceOf(VehicleUsageForbiddenException.class);
    }

    @Test
    void calculate_missingWindForBike_addsWarning() {
        givenBaseFee(City.TARTU, VehicleType.BIKE, "2.50");
        givenWeather(City.TARTU, 5.0, null, "Overcast");
        givenRules(VehicleType.BIKE, bikeRules());

        DeliveryFeeResponse result = deliveryFeeService.calculate(City.TARTU, VehicleType.BIKE, TIME);

        assertThat(result.fee()).isEqualByComparingTo("2.50");
        assertThat(result.warnings()).singleElement().asString().contains("Wind speed");
    }

    @Test
    void calculate_missingWindForScooter_noWarning() {
        givenBaseFee(City.TARTU, VehicleType.SCOOTER, "3.00");
        givenWeather(City.TARTU, 5.0, null, "Overcast");
        givenRules(VehicleType.SCOOTER, List.of(
                rangeRule(VehicleType.SCOOTER, ExtraFeeType.AIR_TEMPERATURE, -10.0, true, 0.0, false, "0.50")));

        DeliveryFeeResponse result = deliveryFeeService.calculate(City.TARTU, VehicleType.SCOOTER, TIME);

        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void calculate_noBaseFee_throwsNotFound() {
        when(baseFeeRepository.findValidAt(City.TARTU, VehicleType.BIKE, TIME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryFeeService.calculate(City.TARTU, VehicleType.BIKE, TIME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No base fee");
    }

    @Test
    void calculate_noWeatherData_throwsNotFound() {
        givenBaseFee(City.TARTU, VehicleType.BIKE, "2.50");
        when(weatherObservationRepository.findFirstByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                "Tartu-Tõravere", TIME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryFeeService.calculate(City.TARTU, VehicleType.BIKE, TIME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No weather data");
    }

    @Test
    void calculate_oldWeatherData_addsWarning() {
        givenBaseFee(City.TARTU, VehicleType.CAR, "3.50");
        givenWeather(City.TARTU, 5.0, 3.0, "Overcast", TIME.minus(Duration.ofHours(3)));
        givenRules(VehicleType.CAR, List.of());

        DeliveryFeeResponse result = deliveryFeeService.calculate(City.TARTU, VehicleType.CAR, TIME);

        assertThat(result.warnings()).singleElement().asString().contains("older than 2 hours");
    }

    @Test
    void calculate_weatherExactly2HoursOld_noWarning() {
        givenBaseFee(City.TARTU, VehicleType.CAR, "3.50");
        givenWeather(City.TARTU, 5.0, 3.0, "Overcast", TIME.minus(Duration.ofHours(2)));
        givenRules(VehicleType.CAR, List.of());

        DeliveryFeeResponse result = deliveryFeeService.calculate(City.TARTU, VehicleType.CAR, TIME);

        assertThat(result.warnings()).isEmpty();
    }

    
    // --- test data helpers ---

    private void givenBaseFee(City city, VehicleType vehicleType, String fee) {
        BaseFee baseFee = BaseFee.builder()
                .city(city)
                .vehicleType(vehicleType)
                .fee(new BigDecimal(fee))
                .validFrom(Instant.EPOCH)
                .build();
        when(baseFeeRepository.findValidAt(city, vehicleType, TIME)).thenReturn(Optional.of(baseFee));
    }

    private void givenWeather(City city, Double temperature, Double windSpeed, String phenomenon) {
        givenWeather(city, temperature, windSpeed, phenomenon, TIME.minusSeconds(600));
    }

    private void givenWeather(City city, Double temperature, Double windSpeed, String phenomenon, Instant observedAt) {
        WeatherObservation weather = WeatherObservation.builder()
                .stationName(city.getStationName())
                .airTemperature(temperature)
                .windSpeed(windSpeed)
                .phenomenon(phenomenon)
                .observedAt(observedAt)
                .build();
        when(weatherObservationRepository.findFirstByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                city.getStationName(), TIME)).thenReturn(Optional.of(weather));
    }

    private void givenRules(VehicleType vehicleType, List<ExtraFeeRule> rules) {
        when(extraFeeRuleRepository.findValidAt(vehicleType, TIME)).thenReturn(rules);
    }

    private static List<ExtraFeeRule> bikeRules() {
        return List.of(
                rangeRule(VehicleType.BIKE, ExtraFeeType.AIR_TEMPERATURE, null, false, -10.0, false, "1.00"),
                rangeRule(VehicleType.BIKE, ExtraFeeType.AIR_TEMPERATURE, -10.0, true, 0.0, false, "0.50"),
                rangeRule(VehicleType.BIKE, ExtraFeeType.WIND_SPEED, 10.0, true, 20.0, true, "0.50"),
                forbiddenRangeRule(VehicleType.BIKE, ExtraFeeType.WIND_SPEED, 20.0, false, null, false),
                phenomenonRule(VehicleType.BIKE, PhenomenonCategory.SNOW, "1.00", false),
                phenomenonRule(VehicleType.BIKE, PhenomenonCategory.RAIN, "0.50", false),
                phenomenonRule(VehicleType.BIKE, PhenomenonCategory.GLAZE, null, true));
    }

    private static ExtraFeeRule rangeRule(VehicleType vehicleType, ExtraFeeType type,
                                          Double min, boolean minInclusive,
                                          Double max, boolean maxInclusive, String fee) {
        return ExtraFeeRule.builder()
                .type(type)
                .vehicleType(vehicleType)
                .minValue(min)
                .minInclusive(minInclusive)
                .maxValue(max)
                .maxInclusive(maxInclusive)
                .fee(new BigDecimal(fee))
                .validFrom(Instant.EPOCH)
                .build();
    }

    private static ExtraFeeRule forbiddenRangeRule(VehicleType vehicleType, ExtraFeeType type,
                                                   Double min, boolean minInclusive,
                                                   Double max, boolean maxInclusive) {
        return ExtraFeeRule.builder()
                .type(type)
                .vehicleType(vehicleType)
                .minValue(min)
                .minInclusive(minInclusive)
                .maxValue(max)
                .maxInclusive(maxInclusive)
                .forbidden(true)
                .validFrom(Instant.EPOCH)
                .build();
    }

    private static ExtraFeeRule phenomenonRule(VehicleType vehicleType, PhenomenonCategory category,
                                               String fee, boolean forbidden) {
        return ExtraFeeRule.builder()
                .type(ExtraFeeType.WEATHER_PHENOMENON)
                .vehicleType(vehicleType)
                .phenomenonCategory(category)
                .fee(fee == null ? null : new BigDecimal(fee))
                .forbidden(forbidden)
                .validFrom(Instant.EPOCH)
                .build();
    }
}