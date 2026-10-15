package deliveryfee.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BaseFeeTest {

    private static final Instant CHANGE_TIME = Instant.parse("2026-10-15T12:00:00Z");

    @Test
    void isCurrent_newFee_isTrue() {
        assertThat(fee().isCurrent()).isTrue();
    }

    @Test
    void isCurrent_expiredFee_isFalse() {
        BaseFee fee = fee();
        fee.expire(CHANGE_TIME);

        assertThat(fee.isCurrent()).isFalse();
    }

    @Test
    void replaceFee_expiresOldAndCreatesNextVersionAtSameMoment() {
        BaseFee old = fee();

        BaseFee next = old.replaceFee(new BigDecimal("3.20"), CHANGE_TIME);

        assertThat(old.getValidTo()).isEqualTo(CHANGE_TIME);
        assertThat(next.getValidFrom()).isEqualTo(CHANGE_TIME);
        assertThat(next.getValidTo()).isNull();
        assertThat(next.getCity()).isEqualTo(City.TARTU);
        assertThat(next.getVehicleType()).isEqualTo(VehicleType.BIKE);
        assertThat(next.getFee()).isEqualByComparingTo("3.20");
    }

    private static BaseFee fee() {
        return BaseFee.builder()
                .city(City.TARTU)
                .vehicleType(VehicleType.BIKE)
                .fee(new BigDecimal("2.50"))
                .validFrom(Instant.EPOCH)
                .build();
    }
}