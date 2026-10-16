package deliveryfee.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Request for changing an existing extra fee rule.
 * Rule type, vehicle type and phenomenon category cannot be changed.
 *
 * @param minValue new lower bound, null if unbounded; must be empty for phenomenon rules
 * @param minInclusive whether the lower bound itself is included
 * @param maxValue new upper bound, null if unbounded; must be empty for phenomenon rules
 * @param maxInclusive whether the upper bound itself is included
 * @param fee new extra fee in euros, must be empty for forbidding rules
 * @param forbidden whether the rule forbids using the vehicle type
 */
@Schema(description = "New values for an existing extra fee rule")
public record ExtraFeeRuleUpdateRequest(
        @Schema(description = "New lower bound, null if unbounded; must be empty for phenomenon rules") 
        Double minValue,

        @Schema(description = "Whether the lower bound itself is included") 
        boolean minInclusive,

        @Schema(description = "New upper bound, null if unbounded; must be empty for phenomenon rules") 
        Double maxValue,

        @Schema(description = "Whether the upper bound itself is included") 
        boolean maxInclusive,

        @Schema(description = "New extra fee in euros, must be empty for forbidding rules") 
        @PositiveOrZero @Digits(integer = 8, fraction = 2) 
        BigDecimal fee,

        @Schema(description = "Whether the rule forbids using the vehicle type") 
        boolean forbidden
) {

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