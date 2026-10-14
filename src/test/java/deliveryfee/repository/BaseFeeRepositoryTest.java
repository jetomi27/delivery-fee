package deliveryfee.repository;

import static org.assertj.core.api.Assertions.assertThat;

import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class BaseFeeRepositoryTest {

    private static final Instant JAN_1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant CHANGE_TIME = Instant.parse("2026-10-14T12:00:00Z");

    @Autowired
    private BaseFeeRepository baseFeeRepository;

    @Test
    void findValidAt_currentFee_isFound() {
        save(City.TALLINN, VehicleType.CAR, "4.00", JAN_1);

        Optional<BaseFee> result = baseFeeRepository.findValidAt(
                City.TALLINN, VehicleType.CAR, CHANGE_TIME);

        assertThat(result).isPresent();
        assertThat(result.get().getFee()).isEqualByComparingTo("4.00");
    }

    @Test
    void findValidAt_beforeValidFrom_isEmpty() {
        save(City.TALLINN, VehicleType.CAR, "4.00", JAN_1);

        Optional<BaseFee> result = baseFeeRepository.findValidAt(
                City.TALLINN, VehicleType.CAR, JAN_1.minusSeconds(1));

        assertThat(result).isEmpty();
    }

    @Test
    void findValidAt_exactlyAtValidFrom_isFound() {
        save(City.TALLINN, VehicleType.CAR, "4.00", JAN_1);

        Optional<BaseFee> result = baseFeeRepository.findValidAt(
                City.TALLINN, VehicleType.CAR, JAN_1);

        assertThat(result).isPresent();
    }

    @Test
    void findValidAt_feeReplaced_returnsFeeValidAtThatTime() {
        BaseFee oldFee = save(City.TALLINN, VehicleType.CAR, "4.00", JAN_1);
        oldFee.expire(CHANGE_TIME);
        baseFeeRepository.save(oldFee);
        save(City.TALLINN, VehicleType.CAR, "4.50", CHANGE_TIME);

        assertThat(feeAt(CHANGE_TIME.minusSeconds(1))).isEqualByComparingTo("4.00");
        assertThat(feeAt(CHANGE_TIME)).isEqualByComparingTo("4.50");
        assertThat(feeAt(CHANGE_TIME.plusSeconds(3600))).isEqualByComparingTo("4.50");
    }

    @Test
    void findValidAt_otherCityOrVehicle_isNotReturned() {
        save(City.TARTU, VehicleType.CAR, "3.50", JAN_1);
        save(City.TALLINN, VehicleType.BIKE, "3.00", JAN_1);

        Optional<BaseFee> result = baseFeeRepository.findValidAt(
                City.TALLINN, VehicleType.CAR, CHANGE_TIME);

        assertThat(result).isEmpty();
    }

    private BaseFee save(City city, VehicleType vehicleType, String fee, Instant validFrom) {
        return baseFeeRepository.save(BaseFee.builder()
                .city(city)
                .vehicleType(vehicleType)
                .fee(new BigDecimal(fee))
                .validFrom(validFrom)
                .build());
    }

    private BigDecimal feeAt(Instant time) {
        return baseFeeRepository.findValidAt(City.TALLINN, VehicleType.CAR, time)
                .orElseThrow()
                .getFee();
    }
}