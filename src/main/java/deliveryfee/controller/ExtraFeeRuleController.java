package deliveryfee.controller;

import deliveryfee.dto.ErrorResponse;
import deliveryfee.dto.ExtraFeeRuleRequest;
import deliveryfee.dto.ExtraFeeRuleResponse;
import deliveryfee.dto.ExtraFeeRuleUpdateRequest;
import deliveryfee.service.ExtraFeeRuleService;
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
 * REST endpoints for managing weather extra fee rules.
 */
@Tag(name = "Extra fee rules", description = "Management of weather based extra fees")
@RestController
@RequestMapping(value = "/api/extra-fee-rules")
public class ExtraFeeRuleController {

    private final ExtraFeeRuleService extraFeeRuleService;

    public ExtraFeeRuleController(ExtraFeeRuleService extraFeeRuleService) {
        this.extraFeeRuleService = extraFeeRuleService;
    }

    /**
     * Returns all currently valid extra fee rules.
     *
     * @return current rules
     */
    @Operation(summary = "List current extra fee rules")
    @GetMapping
    public List<ExtraFeeRuleResponse> getCurrentRules() {
        return extraFeeRuleService.getCurrentRules();
    }

    /**
     * Returns an extra fee rule by id, including expired versions.
     *
     * @param id rule id
     * @return extra fee rule
     */
    @Operation(summary = "Get extra fee rule by id", description = "Also returns expired rule versions")
    @ApiResponse(responseCode = "200", description = "Rule found")
    @ApiResponse(responseCode = "404", description = "Rule not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ExtraFeeRuleResponse getById(@PathVariable Long id) {
        return extraFeeRuleService.getById(id);
    }

    /**
     * Creates a new extra fee rule valid from now.
     *
     * @param request rule parameters
     * @return created rule
     */
    @Operation(summary = "Create extra fee rule",
            description = "Temperature and wind rules need a range, phenomenon rules need a category. "
                    + "A forbidding rule has no fee. The rule must not overlap with current rules "
                    + "of the same type and vehicle type.")
    @ApiResponse(responseCode = "201", description = "Rule created")
    @ApiResponse(responseCode = "400", description = "Invalid or inconsistent rule",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Rule overlaps with a current rule",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ExtraFeeRuleResponse create(@Valid @RequestBody ExtraFeeRuleRequest request) {
        return extraFeeRuleService.create(request);
    }

    /**
     * Changes a current extra fee rule.
     *
     * @param id id of the current rule
     * @param request new bounds, fee and forbidden flag
     * @return new version of the rule
     */
    @Operation(summary = "Change extra fee rule",
            description = "Expires the current rule and creates a new version with the new values. "
                    + "Rule type, vehicle type and phenomenon category stay the same. "
                    + "The response contains the new version with a new id.")
    @ApiResponse(responseCode = "200", description = "Rule changed")
    @ApiResponse(responseCode = "400", description = "Invalid values or bounds do not fit the rule type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Rule not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Rule is expired or overlaps with another current rule",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExtraFeeRuleResponse update(@PathVariable Long id, @Valid @RequestBody ExtraFeeRuleUpdateRequest request) {
        return extraFeeRuleService.update(id, request);
    }

    /**
     * Ends the validity of a current extra fee rule.
     *
     * @param id id of the current rule
     */
    @Operation(summary = "Delete extra fee rule",
            description = "Expires the rule instead of removing it, so calculations "
                    + "for earlier times are not affected. From now on the rule is no longer applied.")
    @ApiResponse(responseCode = "204", description = "Rule expired")
    @ApiResponse(responseCode = "404", description = "Rule not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Rule is already expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        extraFeeRuleService.delete(id);
    }
}