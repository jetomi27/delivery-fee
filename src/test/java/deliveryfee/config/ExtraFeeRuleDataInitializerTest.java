package deliveryfee.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import deliveryfee.repository.ExtraFeeRuleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtraFeeRuleDataInitializerTest {

    @Mock
    private ExtraFeeRuleRepository extraFeeRuleRepository;

    @InjectMocks
    private ExtraFeeRuleDataInitializer initializer;

    @Captor
    private ArgumentCaptor<List<ExtraFeeRule>> savedCaptor;

    @Test
    void run_emptyTable_insertsAllDefaultRules() {
        List<ExtraFeeRule> rules = runOnEmptyTable();

        assertThat(rules).hasSize(18);
        assertThat(rules).allMatch(rule -> rule.getValidTo() == null);
        assertThat(rules).noneMatch(rule -> rule.getVehicleType() == VehicleType.CAR);
    }

    @Test
    void run_emptyTable_windRulesOnlyForBike() {
        List<ExtraFeeRule> windRules = runOnEmptyTable().stream()
                .filter(rule -> rule.getType() == ExtraFeeType.WIND_SPEED)
                .toList();

        assertThat(windRules).hasSize(2);
        assertThat(windRules).allMatch(rule -> rule.getVehicleType() == VehicleType.BIKE);
    }

    @Test
    void run_emptyTable_windAbove20IsForbiddenAndExclusive() {
        ExtraFeeRule forbiddenWind = runOnEmptyTable().stream()
                .filter(rule -> rule.getType() == ExtraFeeType.WIND_SPEED && rule.isForbidden())
                .findFirst()
                .orElseThrow();

        assertThat(forbiddenWind.getMinValue()).isEqualTo(20.0);
        assertThat(forbiddenWind.isMinInclusive()).isFalse();
        assertThat(forbiddenWind.getMaxValue()).isNull();
        assertThat(forbiddenWind.getFee()).isNull();
    }

    @Test
    void run_emptyTable_temperatureFromMinus10To0IncludesMinus10ExcludesZero() {
        ExtraFeeRule rule = runOnEmptyTable().stream()
                .filter(r -> r.getType() == ExtraFeeType.AIR_TEMPERATURE
                        && r.getVehicleType() == VehicleType.BIKE
                        && Double.valueOf(-10.0).equals(r.getMinValue()))
                .findFirst()
                .orElseThrow();

        assertThat(rule.isMinInclusive()).isTrue();
        assertThat(rule.getMaxValue()).isEqualTo(0.0);
        assertThat(rule.isMaxInclusive()).isFalse();
        assertThat(rule.getFee()).isEqualByComparingTo("0.50");
    }

    @Test
    void run_emptyTable_glazeHailThunderAreForbidden() {
        List<PhenomenonCategory> forbiddenCategories = runOnEmptyTable().stream()
                .filter(rule -> rule.getType() == ExtraFeeType.WEATHER_PHENOMENON)
                .filter(ExtraFeeRule::isForbidden)
                .filter(rule -> rule.getVehicleType() == VehicleType.SCOOTER)
                .map(ExtraFeeRule::getPhenomenonCategory)
                .toList();

        assertThat(forbiddenCategories).containsExactlyInAnyOrder(
                PhenomenonCategory.GLAZE, PhenomenonCategory.HAIL, PhenomenonCategory.THUNDER);
    }

    @Test
    void run_rulesAlreadyExist_insertsNothing() {
        when(extraFeeRuleRepository.count()).thenReturn(18L);

        initializer.run(null);

        verify(extraFeeRuleRepository, never()).saveAll(anyList());
    }

    private List<ExtraFeeRule> runOnEmptyTable() {
        when(extraFeeRuleRepository.count()).thenReturn(0L);
        initializer.run(null);
        verify(extraFeeRuleRepository).saveAll(savedCaptor.capture());
        return savedCaptor.getValue();
    }
}