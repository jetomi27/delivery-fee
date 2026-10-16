package deliveryfee.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExtraFeeRuleUpdateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequest_hasNoViolations() {
        assertThat(violations(request(10.0, true, 20.0, true, "0.70", false))).isEmpty();
    }

    @Test
    void validForbiddenRequest_hasNoViolations() {
        assertThat(violations(request(20.0, false, null, false, null, true))).isEmpty();
    }

    @Test
    void regularRuleWithoutFee_violatesFeeMatchingForbidden() {
        assertThat(violations(request(10.0, true, 20.0, true, null, false)))
                .containsExactly("feeMatchingForbidden");
    }

    @Test
    void forbiddenRuleWithFee_violatesFeeMatchingForbidden() {
        assertThat(violations(request(20.0, false, null, false, "1.00", true)))
                .containsExactly("feeMatchingForbidden");
    }

    @Test
    void minGreaterThanMax_violatesRangeOrdered() {
        assertThat(violations(request(20.0, true, 10.0, true, "0.50", false)))
                .containsExactly("rangeOrdered");
    }

    @Test
    void negativeFee_violatesPositiveOrZero() {
        assertThat(violations(request(10.0, true, 20.0, true, "-0.50", false)))
                .containsExactly("fee");
    }

    @Test
    void feeWithThreeDecimals_violatesDigits() {
        assertThat(violations(request(10.0, true, 20.0, true, "0.555", false)))
                .containsExactly("fee");
    }

       @Test
    void onlyUpperBound_isValid() {
        assertThat(violations(request(null, false, -10.0, false, "1.00", false))).isEmpty();
    }

    @Test
    void equalBoundsBothInclusive_isValidSinglePoint() {
        assertThat(violations(request(5.0, true, 5.0, true, "0.50", false))).isEmpty();
    }

    @Test
    void equalBoundsLowerExclusive_violatesRangeOrdered() {
        assertThat(violations(request(5.0, false, 5.0, true, "0.50", false)))
                .containsExactly("rangeOrdered");
    }

    @Test
    void equalBoundsUpperExclusive_violatesRangeOrdered() {
        assertThat(violations(request(5.0, true, 5.0, false, "0.50", false)))
                .containsExactly("rangeOrdered");
    }

    private Set<String> violations(ExtraFeeRuleUpdateRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static ExtraFeeRuleUpdateRequest request(Double min, boolean minInclusive, Double max,
                                                     boolean maxInclusive, String fee, boolean forbidden) {
        return new ExtraFeeRuleUpdateRequest(min, minInclusive, max, maxInclusive,
                fee == null ? null : new BigDecimal(fee), forbidden);
    }
}