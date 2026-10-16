package deliveryfee.controller;

import deliveryfee.dto.ErrorResponse;
import deliveryfee.dto.ExtraFeeRuleResponse;
import deliveryfee.service.ExtraFeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}