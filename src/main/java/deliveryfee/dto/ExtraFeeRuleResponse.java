package deliveryfee.dto;

import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Weather extra fee rule returned by the REST API.
 *
 * @param id identifier of this rule version
 * @param type weather condition the rule is based on
 * @param vehicleType vehicle type the rule applies to
 * @param minValue lower bound of the range, null if unbounded
 * @param minInclusive whether the lower bound itself is included
 * @param maxValue upper bound of the range, null if unbounded
 * @param maxInclusive whether the upper bound itself is included
 * @param phenomenonCategory phenomenon category for phenomenon rules
 * @param fee extra fee in euros, null for forbidding rules
 * @param forbidden whether the rule forbids using the vehicle type
 * @param validFrom moment from which the rule is valid
 * @param validTo moment from which the rule is no longer valid, null if the rule is current
 */
@Schema(description = "Weather extra fee rule")
public record ExtraFeeRuleResponse(
        @Schema(description = "Identifier of this rule version", example = "3") 
        Long id,

        @Schema(description = "Weather condition the rule is based on", example = "WIND_SPEED") 
        ExtraFeeType type,

        @Schema(description = "Vehicle type the rule applies to", example = "BIKE") 
        VehicleType vehicleType,

        @Schema(description = "Lower bound of the range, null if unbounded", example = "10.0") 
        Double minValue,

        @Schema(description = "Whether the lower bound itself is included", example = "true") 
        boolean minInclusive,

        @Schema(description = "Upper bound of the range, null if unbounded", example = "20.0") 
        Double maxValue,

        @Schema(description = "Whether the upper bound itself is included", example = "true") 
        boolean maxInclusive,
        
        @Schema (description = "Phenomenon category for phenomenon rules", example = "RAIN")
        PhenomenonCategory phenomenonCategory,

        @Schema(description = "Extra fee in euros, null for forbidding rules", example = "0.50") 
        BigDecimal fee,

        @Schema(description = "Whether the rule forbids using the vehicle type", example = "false")
        boolean forbidden,

        @Schema(description = "Moment from which the rule is valid", example = "2024-01-01T00:00:00Z")
        Instant validFrom,

        @Schema(description = "Moment from which the rule is no longer valid, null if the rule is current", 
                example = "2024-12-31T23:59:59Z")
        Instant validTo
) {
}