package deliveryfee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Regional base fee for a city and vehicle type, valid during a period of time
 */
@Entity
@Table(name = "base_fee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // store enum names instead of ordinals, so reordering enum values does not change stored data
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private City city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    // up to 10 digits in total, 2 of them after the decimal point
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false)
    private Instant validFrom;

    // null means valid now
    private Instant validTo;

    @Builder
    public BaseFee(City city, VehicleType vehicleType, BigDecimal fee, Instant validFrom) {
        this.city = city;
        this.vehicleType = vehicleType;
        this.fee = fee;
        this.validFrom = validFrom;
    }

    /**
     * Ends the validity of this fee. Used when the fee is replaced or deleted,
     * so that calculations for earlier times still find it.
     *
     * @param time moment from which the fee is no longer valid
     */
    public void expire(Instant time) {
        this.validTo = time;
    }
}