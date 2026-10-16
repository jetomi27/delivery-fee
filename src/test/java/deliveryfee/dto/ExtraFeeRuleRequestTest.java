package deliveryfee.dto;

import static org.assertj.core.api.Assertions.assertThat;

import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExtraFeeRuleRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // --- valid rules ---

    @Test
    void validRangeRule_hasNoViolations() {
        assertThat(violations(range(10.0, true, 20.0, true, "0.50", false))).isEmpty();
    }

    @Test
    void validRangeRuleWithOnlyLowerBound_hasNoViolations() {
        assertThat(violations(range(20.0, false, null, false, null, true))).isEmpty();
    }

    @Test
    void validPhenomenonRule_hasNoViolations() {
        assertThat(violations(phenomenon(PhenomenonCategory.SNOW, null, null, "1.00", false))).isEmpty();
    }

    @Test
    void validForbiddenPhenomenonRule_hasNoViolations() {
        assertThat(violations(phenomenon(PhenomenonCategory.GLAZE, null, null, null, true))).isEmpty();
    }

    // --- isFieldsMatchingType ---

    @Test
    void phenomenonRuleWithBounds_violatesFieldsMatchingType() {
        assertThat(violations(phenomenon(PhenomenonCategory.SNOW, 5.0, null, "1.00", false)))
                .containsExactly("fieldsMatchingType");
    }

    @Test
    void phenomenonRuleWithoutCategory_violatesFieldsMatchingType() {
        assertThat(violations(phenomenon(null, null, null, "1.00", false)))
                .containsExactly("fieldsMatchingType");
    }

    @Test
    void rangeRuleWithoutBounds_violatesFieldsMatchingType() {
        assertThat(violations(range(null, false, null, false, "0.50", false)))
                .containsExactly("fieldsMatchingType");
    }

    @Test
    void rangeRuleWithCategory_violatesFieldsMatchingType() {
        ExtraFeeRuleRequest request = new ExtraFeeRuleRequest(ExtraFeeType.WIND_SPEED, VehicleType.BIKE,
                10.0, true, 20.0, true, PhenomenonCategory.SNOW, new BigDecimal("0.50"), false);

        assertThat(violations(request)).containsExactly("fieldsMatchingType");
    }

    @Test
    void missingType_reportsOnlyNotNull() {
        ExtraFeeRuleRequest request = new ExtraFeeRuleRequest(null, VehicleType.BIKE,
                10.0, true, 20.0, true, null, new BigDecimal("0.50"), false);

        assertThat(violations(request)).containsExactly("type");
    }

    // --- isFeeMatchingForbidden ---

    @Test
    void regularRuleWithoutFee_violatesFeeMatchingForbidden() {
        assertThat(violations(range(10.0, true, 20.0, true, null, false)))
                .containsExactly("feeMatchingForbidden");
    }

    @Test
    void forbiddenRuleWithFee_violatesFeeMatchingForbidden() {
        assertThat(violations(range(20.0, false, null, false, "1.00", true)))
                .containsExactly("feeMatchingForbidden");
    }

    // --- isRangeOrdered ---

    @Test
    void minGreaterThanMax_violatesRangeOrdered() {
        assertThat(violations(range(20.0, true, 10.0, true, "0.50", false)))
                .containsExactly("rangeOrdered");
    }

    @Test
    void equalBoundsBothInclusive_isValidSinglePoint() {
        assertThat(violations(range(5.0, true, 5.0, true, "0.50", false))).isEmpty();
    }

    @Test
    void equalBoundsNotBothInclusive_violatesRangeOrdered() {
        assertThat(violations(range(5.0, true, 5.0, false, "0.50", false)))
                .containsExactly("rangeOrdered");
    }

    @Test
    void validRangeRuleWithOnlyUpperBound_hasNoViolations() {
        assertThat(violations(range(null, false, -10.0, false, "1.00", false))).isEmpty();
    }

    @Test
    void phenomenonRuleWithUpperBound_violatesFieldsMatchingType() {
        assertThat(violations(phenomenon(PhenomenonCategory.SNOW, null, 5.0, "1.00", false)))
                .containsExactly("fieldsMatchingType");
    }

    @Test
    void equalBoundsLowerExclusive_violatesRangeOrdered() {
        assertThat(violations(range(5.0, false, 5.0, true, "0.50", false)))
                .containsExactly("rangeOrdered");
    }

    // --- helpers ---

    private Set<String> violations(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static ExtraFeeRuleRequest range(Double min, boolean minInclusive, Double max, boolean maxInclusive,
                                             String fee, boolean forbidden) {
        return new ExtraFeeRuleRequest(ExtraFeeType.WIND_SPEED, VehicleType.BIKE,
                min, minInclusive, max, maxInclusive, null,
                fee == null ? null : new BigDecimal(fee), forbidden);
    }

    private static ExtraFeeRuleRequest phenomenon(PhenomenonCategory category, Double min, Double max,
                                                  String fee, boolean forbidden) {
        return new ExtraFeeRuleRequest(ExtraFeeType.WEATHER_PHENOMENON, VehicleType.BIKE,
                min, false, max, false, category,
                fee == null ? null : new BigDecimal(fee), forbidden);
    }
}