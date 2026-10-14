package deliveryfee.config;

import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import deliveryfee.repository.ExtraFeeRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads default weather extra fee rules on first startup
 */
@Slf4j
@Component
public class ExtraFeeRuleDataInitializer implements ApplicationRunner {

    // 1970
    private static final Instant DEFAULT_VALID_FROM = Instant.EPOCH;

    private final ExtraFeeRuleRepository extraFeeRuleRepository;

    public ExtraFeeRuleDataInitializer(ExtraFeeRuleRepository extraFeeRuleRepository) {
        this.extraFeeRuleRepository = extraFeeRuleRepository;
    }

    /**
     * Inserts the default extra fee rules if the rule table is empty.
     *
     * @param args application arguments, not used
     */
    @Override
    public void run(ApplicationArguments args) {
        if (extraFeeRuleRepository.count() > 0) {
            log.info("Extra fee rules already exist, skipping default data");
            return;
        }

        List<ExtraFeeRule> rules = new ArrayList<>();

        for (VehicleType vehicle : List.of(VehicleType.SCOOTER, VehicleType.BIKE)) {
            // Air temperature: below -10 is 1 EUR, from -10 (inclusive) to 0 (exclusive) is 0.5 EUR
            rules.add(range(ExtraFeeType.AIR_TEMPERATURE, vehicle, null, false, -10.0, false, "1.00"));
            rules.add(range(ExtraFeeType.AIR_TEMPERATURE, vehicle, -10.0, true, 0.0, false, "0.50"));

            rules.add(phenomenon(vehicle, PhenomenonCategory.SNOW, "1.00"));
            rules.add(phenomenon(vehicle, PhenomenonCategory.SLEET, "1.00"));
            rules.add(phenomenon(vehicle, PhenomenonCategory.RAIN, "0.50"));
            rules.add(forbiddenPhenomenon(vehicle, PhenomenonCategory.GLAZE));
            rules.add(forbiddenPhenomenon(vehicle, PhenomenonCategory.HAIL));
            rules.add(forbiddenPhenomenon(vehicle, PhenomenonCategory.THUNDER));
        }

        // Wind speed applies to bikes only: 10 to 20 inclusive is 0.5 EUR, above 20 is forbidden
        rules.add(range(ExtraFeeType.WIND_SPEED, VehicleType.BIKE, 10.0, true, 20.0, true, "0.50"));
        rules.add(forbiddenRange(ExtraFeeType.WIND_SPEED, VehicleType.BIKE, 20.0, false, null, false));

        extraFeeRuleRepository.saveAll(rules);
        log.info("Inserted {} default extra fee rules", rules.size());
    }

    private ExtraFeeRule range(ExtraFeeType type, VehicleType vehicle,
                               Double min, boolean minInclusive,
                               Double max, boolean maxInclusive, String fee) {
        return ExtraFeeRule.builder()
                .type(type)
                .vehicleType(vehicle)
                .minValue(min)
                .minInclusive(minInclusive)
                .maxValue(max)
                .maxInclusive(maxInclusive)
                .fee(new BigDecimal(fee))
                .validFrom(DEFAULT_VALID_FROM)
                .build();
    }

    private ExtraFeeRule forbiddenRange(ExtraFeeType type, VehicleType vehicle,
                                        Double min, boolean minInclusive,
                                        Double max, boolean maxInclusive) {
        return ExtraFeeRule.builder()
                .type(type)
                .vehicleType(vehicle)
                .minValue(min)
                .minInclusive(minInclusive)
                .maxValue(max)
                .maxInclusive(maxInclusive)
                .forbidden(true)
                .validFrom(DEFAULT_VALID_FROM)
                .build();
    }

    private ExtraFeeRule phenomenon(VehicleType vehicle, PhenomenonCategory category, String fee) {
        return ExtraFeeRule.builder()
                .type(ExtraFeeType.WEATHER_PHENOMENON)
                .vehicleType(vehicle)
                .phenomenonCategory(category)
                .fee(new BigDecimal(fee))
                .validFrom(DEFAULT_VALID_FROM)
                .build();
    }

    private ExtraFeeRule forbiddenPhenomenon(VehicleType vehicle, PhenomenonCategory category) {
        return ExtraFeeRule.builder()
                .type(ExtraFeeType.WEATHER_PHENOMENON)
                .vehicleType(vehicle)
                .phenomenonCategory(category)
                .forbidden(true)
                .validFrom(DEFAULT_VALID_FROM)
                .build();
    }
}