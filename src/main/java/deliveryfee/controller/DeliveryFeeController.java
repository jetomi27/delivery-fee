package deliveryfee.controller;

import deliveryfee.dto.DeliveryFeeResponse;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.service.DeliveryFeeService;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for calculating courier delivery fees
 */
@RestController
@RequestMapping("/api/delivery-fee")
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    public DeliveryFeeController(DeliveryFeeService deliveryFeeService) {
        this.deliveryFeeService = deliveryFeeService;
    }

    /**
     * Calculates the delivery fee for a city and vehicle type.
     *
     * @param city delivery city
     * @param vehicleType courier vehicle type
     * @param datetime optional moment in ISO 8601 format with offset, e.g. 2026-10-14T12:00:00Z;
     *                 if given, rules and weather valid at that moment are used, otherwise the latest ones
     * @return calculated fee and warnings about missing weather data
     */
    @GetMapping
    public DeliveryFeeResponse getDeliveryFee(
            @RequestParam City city,
            @RequestParam VehicleType vehicleType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime datetime) {
        Instant time = datetime != null ? datetime.toInstant() : Instant.now();
        return deliveryFeeService.calculate(city, vehicleType, time);
    }
}