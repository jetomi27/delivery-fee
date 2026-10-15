package deliveryfee.controller;

import deliveryfee.dto.BaseFeeResponse;
import deliveryfee.dto.ErrorResponse;
import deliveryfee.service.BaseFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for managing regional base fees
 */
@Tag(name = "Base fees", description = "Management of regional base fees")
@RestController
@RequestMapping("/api/base-fees")
public class BaseFeeController {

    private final BaseFeeService baseFeeService;

    public BaseFeeController(BaseFeeService baseFeeService) {
        this.baseFeeService = baseFeeService;
    }

    /**
     * Returns all currently valid base fees.
     *
     * @return current base fees
     */
    @Operation(summary = "List current base fees")
    @GetMapping
    public List<BaseFeeResponse> getCurrentFees() {
        return baseFeeService.getCurrentFees();
    }

    /**
     * Returns a base fee by id, including expired versions.
     *
     * @param id base fee id
     * @return base fee
     */
    @Operation(summary = "Get base fee by id", description = "Also returns expired fee versions")
    @ApiResponse(responseCode = "200", description = "Base fee found")
    @ApiResponse(responseCode = "404", description = "Base fee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public BaseFeeResponse getById(@PathVariable Long id) {
        return baseFeeService.getById(id);
    }
}