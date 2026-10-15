package deliveryfee.dto;

import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Request for creating a regional base fee.
 *
 * @param city delivery city
 * @param vehicleType courier vehicle type
 * @param fee base fee in euros
 */
@Schema(description = "New regional base fee")
public record BaseFeeRequest(
        @Schema(description = "Delivery city", example = "TARTU") 
        @NotNull City city,

        @Schema(description = "Courier vehicle type", example = "BIKE")
        @NotNull VehicleType vehicleType,

        @Schema(description = "Base fee in euros", example = "2.50")
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2)
        BigDecimal fee
) {
}