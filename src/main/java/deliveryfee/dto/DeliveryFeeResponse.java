package deliveryfee.dto;

import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calculated delivery fee with warnings about weather data that could not be taken into account.
 *
 * @param city delivery city
 * @param vehicleType courier vehicle type
 * @param fee total delivery fee in euros
 * @param warnings messages about missing weather data, empty if all data was available
 */
@Schema(description = "Calculated delivery fee")
public record DeliveryFeeResponse(
        @Schema(description = "Delivery city", example = "TARTU")
        City city,

        @Schema(description = "Courier vehicle type", example = "BIKE")
        VehicleType vehicleType,

        @Schema(description = "Total delivery fee in euros", example = "10.50")
        BigDecimal fee,

        @Schema(description = "Messages about missing weather data, empty if all data was available",
                example = "[\"Air temperature data is missing, air temperature rules were not applied\"]"
        )
        List<String> warnings
) {
}