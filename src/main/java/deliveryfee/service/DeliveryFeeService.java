package deliveryfee.service;

import deliveryfee.dto.DeliveryFeeResponse;
import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.VehicleType;
import deliveryfee.entity.WeatherObservation;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.exception.VehicleUsageForbiddenException;
import deliveryfee.repository.BaseFeeRepository;
import deliveryfee.repository.ExtraFeeRuleRepository;
import deliveryfee.repository.WeatherObservationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates delivery fees from regional base fees, weather data and extra fee rules
 */
@Service
public class DeliveryFeeService {

    private final BaseFeeRepository baseFeeRepository;
    private final WeatherObservationRepository weatherObservationRepository;
    private final ExtraFeeRuleRepository extraFeeRuleRepository;

    public DeliveryFeeService(BaseFeeRepository baseFeeRepository,
                              WeatherObservationRepository weatherObservationRepository,
                              ExtraFeeRuleRepository extraFeeRuleRepository) {
        this.baseFeeRepository = baseFeeRepository;
        this.weatherObservationRepository = weatherObservationRepository;
        this.extraFeeRuleRepository = extraFeeRuleRepository;
    }

    /**
     * Calculates the delivery fee using the rules and weather data valid at the given time.
     *
     * @param city delivery city
     * @param vehicleType courier vehicle type
     * @param time moment to calculate the fee for, current time for a regular request
     * @return total delivery fee
     * @throws ResourceNotFoundException if there is no base fee or weather data for that time
     * @throws VehicleUsageForbiddenException if weather conditions forbid the vehicle type
     */
    @Transactional(readOnly = true)
    public DeliveryFeeResponse calculate(City city, VehicleType vehicleType, Instant time) {
        BigDecimal baseFee = findBaseFee(city, vehicleType, time);
        WeatherObservation weather = findWeather(city, time);
        List<ExtraFeeRule> rules = extraFeeRuleRepository.findValidAt(vehicleType, time);

        BigDecimal extraFees = calculateExtraFees(rules, weather);
        List<String> warnings = collectWarnings(rules, weather);
        return new DeliveryFeeResponse(city, vehicleType, baseFee.add(extraFees), warnings);
    }

    private BigDecimal findBaseFee(City city, VehicleType vehicleType, Instant time) {
        return baseFeeRepository.findValidAt(city, vehicleType, time)
                .map(BaseFee::getFee)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No base fee for " + city + " and " + vehicleType + " at " + time));
    }

    private WeatherObservation findWeather(City city, Instant time) {
        return weatherObservationRepository
                .findFirstByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                        city.getStationName(), time)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No weather data for " + city + " at " + time));
    }

    private BigDecimal calculateExtraFees(List<ExtraFeeRule> rules, WeatherObservation weather) {
        BigDecimal total = BigDecimal.ZERO;
        for (ExtraFeeRule rule : rules) {
            if (!rule.appliesTo(weather)) {
                continue;
            }
            if (rule.isForbidden()) {
                throw new VehicleUsageForbiddenException();
            }
            total = total.add(rule.getFee());
        }
        return total;
    }

    private List<String> collectWarnings(List<ExtraFeeRule> rules, WeatherObservation weather) {
        List<String> warnings = new ArrayList<>();
        if (hasRuleOfType(rules, ExtraFeeType.AIR_TEMPERATURE) && weather.getAirTemperature() == null) {
            warnings.add("Air temperature data is missing, air temperature rules were not applied");
        }
        if (hasRuleOfType(rules, ExtraFeeType.WIND_SPEED) && weather.getWindSpeed() == null) {
            warnings.add("Wind speed data is missing, wind speed rules were not applied");
        }
        return warnings;
    }

    private boolean hasRuleOfType(List<ExtraFeeRule> rules, ExtraFeeType type) {
        return rules.stream().anyMatch(rule -> rule.getType() == type);
    }
}