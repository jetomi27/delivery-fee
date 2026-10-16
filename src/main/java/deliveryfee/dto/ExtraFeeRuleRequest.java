package deliveryfee.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Request for creating a weather extra fee rule.
 * Temperature and wind rules use a range, phenomenon rules use a category.
 *
 * @param type weather condition the rule is based on
 * @param vehicleType vehicle type the rule applies to
 * @param minValue lower bound of the range, null if unbounded
 * @param minInclusive whether the lower bound itself is included
 * @param maxValue upper bound of the range, null if unbounded
 * @param maxInclusive whether the upper bound itself is included
 * @param phenomenonCategory phenomenon category for phenomenon rules
 * @param fee extra fee in euros, must be empty for forbidding rules
 * @param forbidden whether the rule forbids using the vehicle type
 */
@Schema(description = "New weather extra fee rule")
public record ExtraFeeRuleRequest(
        @Schema(description = "Weather condition the rule is based on", example = "WIND_SPEED") 
        @NotNull ExtraFeeType type,

        @Schema(description = "Vehicle type the rule applies to", example = "BIKE") 
        @NotNull VehicleType vehicleType,

        @Schema(description = "Lower bound of the range, null if unbounded", example = "10.0") 
        Double minValue,

        @Schema(description = "Whether the lower bound itself is included", example = "true") 
        boolean minInclusive,

        @Schema(description = "Upper bound of the range, null if unbounded", example = "20.0") 
        Double maxValue,

        @Schema(description = "Whether the upper bound itself is included", example = "true") 
        boolean maxInclusive,

        @Schema(description = "Phenomenon category for phenomenon rules") 
        PhenomenonCategory phenomenonCategory,

        @Schema(description = "Extra fee in euros, must be empty for forbidding rules", example = "0.50") 
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal fee,

        @Schema(description = "Whether the rule forbids using the vehicle type", example = "false") 
        boolean forbidden
) {

    @JsonIgnore
    @AssertTrue(message = "range rules need at least one bound and no phenomenon category, "
            + "phenomenon rules need a category and no bounds")
    public boolean isFieldsMatchingType() {
        if (type == null) {
            return true;
        }
        if (type == ExtraFeeType.WEATHER_PHENOMENON) {
            return phenomenonCategory != null && minValue == null && maxValue == null;
        }
        return phenomenonCategory == null && (minValue != null || maxValue != null);
    }

    @JsonIgnore
    @AssertTrue(message = "fee is required for regular rules and must be empty for forbidding rules")
    public boolean isFeeMatchingForbidden() {
        return forbidden ? fee == null : fee != null;
    }

    @JsonIgnore
    @AssertTrue(message = "minValue must not be greater than maxValue")
    public boolean isRangeOrdered() {
        if (minValue == null || maxValue == null) {
            return true;
        }
        if (minValue < maxValue) {
            return true;
        }
        return minValue.equals(maxValue) && minInclusive && maxInclusive;
    }
}