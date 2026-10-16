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
 * Weather based extra fee rule for a vehicle type, valid during a period of time
 * Temperature and wind rules use a value range, phenomenon rules use a category
 */
@Entity
@Table(name = "extra_fee_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExtraFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtraFeeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    // Range bounds for temperature and wind rules, null means unbounded
    private Double minValue;
    private boolean minInclusive;
    private Double maxValue;
    private boolean maxInclusive;

    // Category for weather phenomenon rules
    @Enumerated(EnumType.STRING)
    private PhenomenonCategory phenomenonCategory;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false)
    private boolean forbidden;

    @Column(nullable = false)
    private Instant validFrom;

    private Instant validTo;

    @Builder
    public ExtraFeeRule(ExtraFeeType type, VehicleType vehicleType,
                        Double minValue, boolean minInclusive,
                        Double maxValue, boolean maxInclusive,
                        PhenomenonCategory phenomenonCategory,
                        BigDecimal fee, boolean forbidden, Instant validFrom) {
        this.type = type;
        this.vehicleType = vehicleType;
        this.minValue = minValue;
        this.minInclusive = minInclusive;
        this.maxValue = maxValue;
        this.maxInclusive = maxInclusive;
        this.phenomenonCategory = phenomenonCategory;
        this.fee = fee;
        this.forbidden = forbidden;
        this.validFrom = validFrom;
    }

    /**
    * Ends the validity of this rule. Used when the rule is replaced or deleted,
    * so that calculations for earlier times still find it.
    *
    * @param time moment from which the rule is no longer valid
    */
    public void expire(Instant time) {
        this.validTo = time;
    }

    /**
    * Checks whether this rule applies to the given weather observation.
    * Temperature and wind rules compare the measured value with the range,
    * phenomenon rules compare the phenomenon category.
    * A missing value never matches.
    *
    * @param weather weather observation of the city
    * @return true if the rule applies
    */
    public boolean appliesTo(WeatherObservation weather) {
        return switch (type) {
            case AIR_TEMPERATURE -> isInRange(weather.getAirTemperature());
            case WIND_SPEED -> isInRange(weather.getWindSpeed());
            case WEATHER_PHENOMENON -> PhenomenonCategory.fromPhenomenon(weather.getPhenomenon())
                    .map(category -> category == phenomenonCategory)
                    .orElse(false);
        };
    }

    private boolean isInRange(Double value) {
        if (value == null) {
            return false;
        }
        boolean aboveMin = minValue == null
                || (minInclusive ? value >= minValue : value > minValue);
        boolean belowMax = maxValue == null
                || (maxInclusive ? value <= maxValue : value < maxValue);
        return aboveMin && belowMax;
    }

    /**
     * Checks whether this rule could apply to the same weather as another rule
     * of the same type and vehicle type. Overlapping rules would charge the extra fee twice.
     *
     * @param other another rule
     * @return true if both rules can match the same weather observation
     */
    public boolean overlaps(ExtraFeeRule other) {
        if (type != other.type || vehicleType != other.vehicleType) {
            return false;
        }
        if (type == ExtraFeeType.WEATHER_PHENOMENON) {
            return phenomenonCategory == other.phenomenonCategory;
        }
        // Ranges A (this) and B (other) overlap when both are true:
        //   A starts before B ends  -> A is not entirely to the right of B
        //   B starts before A ends  -> A is not entirely to the left of B
        return startsBeforeEnd(minValue, minInclusive, other.maxValue, other.maxInclusive)
                && startsBeforeEnd(other.minValue, other.minInclusive, maxValue, maxInclusive);
    }

    private static boolean startsBeforeEnd(Double start, boolean startInclusive,
                                        Double end, boolean endInclusive) {
        if (start == null || end == null) {
            return true;
        }
        if (start < end) {
            return true;
        }
        return start.equals(end) && startInclusive && endInclusive;
    }
}