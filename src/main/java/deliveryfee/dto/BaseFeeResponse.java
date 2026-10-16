package deliveryfee.dto;

import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Regional base fee returned by the REST API.
 *
 * @param id identifier of this fee version
 * @param city delivery city
 * @param vehicleType courier vehicle type
 * @param fee base fee in euros
 * @param validFrom moment from which the fee is valid
 * @param validTo moment from which the fee is no longer valid, null if the fee is current
 */
@Schema(description = "Regional base fee")
public record BaseFeeResponse(
        @Schema(description = "Identifier of this fee version", example = "1")
        Long id,

        @Schema(description = "Delivery city", example = "TARTU") 
        City city,

        @Schema(description = "Courier vehicle type", example = "BIKE")
        VehicleType vehicleType,

        @Schema(description = "Base fee in euros", example = "2.50")
        BigDecimal fee,

        @Schema(description = "Moment from which the fee is valid", example = "2024-01-01T00:00:00Z")
        Instant validFrom,

        @Schema(description = "Moment from which the fee is no longer valid, null if the fee is current", 
                example = "2024-12-31T23:59:59Z")
        Instant validTo
) {
}