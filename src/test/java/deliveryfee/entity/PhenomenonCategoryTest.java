package deliveryfee.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhenomenonCategoryTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void fromPhenomenon_missingPhenomenon_returnsEmpty(String phenomenon) {
        assertThat(PhenomenonCategory.fromPhenomenon(phenomenon)).isEmpty();
    }
}