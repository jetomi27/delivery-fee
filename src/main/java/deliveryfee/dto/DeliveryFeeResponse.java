package deliveryfee.dto;

import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
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
public record DeliveryFeeResponse(
        City city,
        VehicleType vehicleType,
        BigDecimal fee,
        List<String> warnings
) {
}