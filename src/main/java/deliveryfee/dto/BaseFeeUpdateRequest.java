package deliveryfee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Request for changing the amount of an existing base fee.
 *
 * @param fee new base fee in euros
 */
@Schema(description = "New amount for an existing base fee")
public record BaseFeeUpdateRequest(
        @Schema(description = "New base fee in euros", example = "2.50")
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2)
        BigDecimal fee
) {
}