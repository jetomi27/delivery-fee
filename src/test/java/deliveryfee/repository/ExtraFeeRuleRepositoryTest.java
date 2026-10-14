package deliveryfee.repository;

import static org.assertj.core.api.Assertions.assertThat;

import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ExtraFeeRuleRepositoryTest {

    private static final Instant JAN_1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant CHANGE_TIME = Instant.parse("2026-10-14T12:00:00Z");

    @Autowired
    private ExtraFeeRuleRepository extraFeeRuleRepository;

    @Test
    void findValidAt_returnsOnlyRulesForVehicleType() {
        save(VehicleType.BIKE, "0.50", JAN_1);
        save(VehicleType.SCOOTER, "0.50", JAN_1);

        List<ExtraFeeRule> result = extraFeeRuleRepository.findValidAt(VehicleType.BIKE, CHANGE_TIME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVehicleType()).isEqualTo(VehicleType.BIKE);
    }

    @Test
    void findValidAt_noRulesForCar_returnsEmptyList() {
        save(VehicleType.BIKE, "0.50", JAN_1);

        List<ExtraFeeRule> result = extraFeeRuleRepository.findValidAt(VehicleType.CAR, CHANGE_TIME);

        assertThat(result).isEmpty();
    }

    @Test
    void findValidAt_expiredRule_isReplacedByNewOne() {
        ExtraFeeRule oldRule = save(VehicleType.BIKE, "0.50", JAN_1);
        oldRule.expire(CHANGE_TIME);
        extraFeeRuleRepository.save(oldRule);
        save(VehicleType.BIKE, "0.80", CHANGE_TIME);

        assertThat(feesAt(CHANGE_TIME.minusSeconds(1))).containsExactly(new BigDecimal("0.50"));
        assertThat(feesAt(CHANGE_TIME)).containsExactly(new BigDecimal("0.80"));
    }

    private ExtraFeeRule save(VehicleType vehicleType, String fee, Instant validFrom) {
        return extraFeeRuleRepository.save(ExtraFeeRule.builder()
                .type(ExtraFeeType.WIND_SPEED)
                .vehicleType(vehicleType)
                .minValue(10.0)
                .minInclusive(true)
                .maxValue(20.0)
                .maxInclusive(true)
                .fee(new BigDecimal(fee))
                .validFrom(validFrom)
                .build());
    }

    private List<BigDecimal> feesAt(Instant time) {
        return extraFeeRuleRepository.findValidAt(VehicleType.BIKE, time).stream()
                .map(ExtraFeeRule::getFee)
                .toList();
    }
}