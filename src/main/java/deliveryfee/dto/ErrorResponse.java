package deliveryfee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error returned by the REST API.
 *
 * @param status HTTP status code
 * @param message human readable error description
 */
@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "422")
        int status,

        @Schema(description = "Human readable error description", example = "Invalid vehicle type")
        String message
) {
}