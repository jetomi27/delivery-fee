package deliveryfee.controller;

import deliveryfee.dto.DeliveryFeeResponse;
import deliveryfee.dto.ErrorResponse;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.service.DeliveryFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Delivery fee", description = "Calculation of courier delivery fees")
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
    @Operation(
            summary = "Calculate delivery fee",
            description = "Total fee = regional base fee + weather extra fees. "
                    + "Uses the latest weather data, or data valid at the given datetime.")
    @ApiResponse(responseCode = "200", description = "Fee calculated")
    @ApiResponse(responseCode = "400", description = "Missing or invalid parameter",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No base fee or weather data for the given time",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Usage of selected vehicle type is forbidden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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