package deliveryfee.controller;

import deliveryfee.dto.BaseFeeRequest;
import deliveryfee.dto.BaseFeeResponse;
import deliveryfee.dto.BaseFeeUpdateRequest;
import deliveryfee.dto.ErrorResponse;
import deliveryfee.service.BaseFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    
    /**
     * Creates a new base fee valid from now.
     *
     * @param request city, vehicle type and fee
     * @return created base fee
     */
    @Operation(summary = "Create base fee",
            description = "Creates a fee for a city and vehicle type that has no current fee. ")
    @ApiResponse(responseCode = "201", description = "Base fee created")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Current base fee already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public BaseFeeResponse create(@Valid @RequestBody BaseFeeRequest request) {
        return baseFeeService.create(request);
    }

    /**
     * Changes the amount of a current base fee.
     *
     * @param id id of the current base fee
     * @param request new amount
     * @return new version of the base fee
     */
    @Operation(summary = "Change base fee",
            description = "Expires the current fee and creates a new version with the new amount. "
                    + "The response contains the new version with a new id.")
    @ApiResponse(responseCode = "200", description = "Base fee changed")
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Base fee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Base fee is expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BaseFeeResponse update(@PathVariable Long id, @Valid @RequestBody BaseFeeUpdateRequest request) {
        return baseFeeService.update(id, request);
    }

    /**
     * Ends the validity of a current base fee.
     *
     * @param id id of the current base fee
     */
    @Operation(summary = "Delete base fee",
            description = "Expires the fee instead of removing it, so calculations "
                    + "for earlier times are not affected. Fee calculation for this city "
                    + "and vehicle type will return 404 until a new fee is created.")
    @ApiResponse(responseCode = "204", description = "Base fee expired")
    @ApiResponse(responseCode = "404", description = "Base fee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Base fee is already expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        baseFeeService.delete(id);
    }
}