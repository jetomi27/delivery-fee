package deliveryfee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import deliveryfee.dto.ExtraFeeRuleRequest;
import deliveryfee.dto.ExtraFeeRuleResponse;
import deliveryfee.dto.ExtraFeeRuleUpdateRequest;
import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.PhenomenonCategory;
import deliveryfee.entity.VehicleType;
import deliveryfee.exception.ConflictException;
import deliveryfee.exception.InvalidRequestException;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.mapper.ExtraFeeRuleMapper;
import deliveryfee.repository.ExtraFeeRuleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExtraFeeRuleServiceTest {

    @Mock
    private ExtraFeeRuleRepository extraFeeRuleRepository;

    @Spy
    private ExtraFeeRuleMapper extraFeeRuleMapper = Mappers.getMapper(ExtraFeeRuleMapper.class);

    @InjectMocks
    private ExtraFeeRuleService extraFeeRuleService;

    @Captor
    private ArgumentCaptor<ExtraFeeRule> savedCaptor;

    @Test
    void getCurrentRules_returnsMappedRules() {
        when(extraFeeRuleRepository.findAllByValidToIsNullOrderByVehicleTypeAscTypeAsc())
                .thenReturn(List.of(windRule(13L, 10.0, true, 20.0, true, "0.50")));

        List<ExtraFeeRuleResponse> result = extraFeeRuleService.getCurrentRules();

        assertThat(result).singleElement().satisfies(rule -> {
            assertThat(rule.id()).isEqualTo(13L);
            assertThat(rule.type()).isEqualTo(ExtraFeeType.WIND_SPEED);
        });
    }

    @Test
    void getById_unknownId_throwsNotFound() {
        when(extraFeeRuleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> extraFeeRuleService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_noOverlap_savesRule() {
        givenCurrentWindRules(VehicleType.SCOOTER);
        returnSavedRule();

        ExtraFeeRuleResponse result = extraFeeRuleService.create(new ExtraFeeRuleRequest(
                ExtraFeeType.WIND_SPEED, VehicleType.SCOOTER, 15.0, true, null, false,
                null, new BigDecimal("0.50"), false));

        verify(extraFeeRuleRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getVehicleType()).isEqualTo(VehicleType.SCOOTER);
        assertThat(savedCaptor.getValue().isCurrent()).isTrue();
        assertThat(result.fee()).isEqualByComparingTo("0.50");
    }

    @Test
    void create_overlapsExistingRule_throwsConflict() {
        givenCurrentWindRules(VehicleType.BIKE, windRule(13L, 10.0, true, 20.0, true, "0.50"));

        assertThatThrownBy(() -> extraFeeRuleService.create(new ExtraFeeRuleRequest(
                ExtraFeeType.WIND_SPEED, VehicleType.BIKE, 15.0, true, 25.0, true,
                null, new BigDecimal("0.30"), false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("13");
        verify(extraFeeRuleRepository, never()).save(any());
    }

    @Test
    void update_currentRule_expiresOldAndSavesNewVersion() {
        ExtraFeeRule current = windRule(13L, 10.0, true, 20.0, true, "0.50");
        when(extraFeeRuleRepository.findById(13L)).thenReturn(Optional.of(current));
        givenCurrentWindRules(VehicleType.BIKE, current);
        returnSavedRule();

        ExtraFeeRuleResponse result = extraFeeRuleService.update(13L,
                new ExtraFeeRuleUpdateRequest(10.0, true, 20.0, true, new BigDecimal("0.70"), false));

        verify(extraFeeRuleRepository, times(2)).save(savedCaptor.capture());
        ExtraFeeRule expired = savedCaptor.getAllValues().get(0);
        ExtraFeeRule next = savedCaptor.getAllValues().get(1);
        assertThat(expired.isCurrent()).isFalse();
        assertThat(next.getValidFrom()).isEqualTo(expired.getValidTo());
        assertThat(next.getType()).isEqualTo(ExtraFeeType.WIND_SPEED);
        assertThat(next.getVehicleType()).isEqualTo(VehicleType.BIKE);
        assertThat(result.fee()).isEqualByComparingTo("0.70");
    }

    @Test
    void update_newVersionOverlapsAnotherRule_throwsConflictAndKeepsOldRule() {
        ExtraFeeRule current = windRule(13L, 10.0, true, 20.0, true, "0.50");
        ExtraFeeRule forbidden = forbiddenWindRule(14L, 20.0, false);
        when(extraFeeRuleRepository.findById(13L)).thenReturn(Optional.of(current));
        givenCurrentWindRules(VehicleType.BIKE, current, forbidden);

        assertThatThrownBy(() -> extraFeeRuleService.update(13L,
                new ExtraFeeRuleUpdateRequest(10.0, true, 25.0, true, new BigDecimal("0.50"), false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("14");
        assertThat(current.isCurrent()).isTrue();
        verify(extraFeeRuleRepository, never()).save(any());
    }

    @Test
    void update_phenomenonRuleWithBounds_throwsInvalidRequest() {
        ExtraFeeRule snow = phenomenonRule(20L, PhenomenonCategory.SNOW);
        when(extraFeeRuleRepository.findById(20L)).thenReturn(Optional.of(snow));

        assertThatThrownBy(() -> extraFeeRuleService.update(20L,
                new ExtraFeeRuleUpdateRequest(5.0, true, null, false, new BigDecimal("1.00"), false)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void update_rangeRuleWithoutBounds_throwsInvalidRequest() {
        ExtraFeeRule current = windRule(13L, 10.0, true, 20.0, true, "0.50");
        when(extraFeeRuleRepository.findById(13L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> extraFeeRuleService.update(13L,
                new ExtraFeeRuleUpdateRequest(null, false, null, false, new BigDecimal("0.50"), false)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void update_expiredRule_throwsConflict() {
        ExtraFeeRule expired = windRule(13L, 10.0, true, 20.0, true, "0.50");
        expired.expire(Instant.parse("2026-10-01T00:00:00Z"));
        when(extraFeeRuleRepository.findById(13L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> extraFeeRuleService.update(13L,
                new ExtraFeeRuleUpdateRequest(10.0, true, 20.0, true, new BigDecimal("0.70"), false)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_currentRule_expiresIt() {
        ExtraFeeRule current = windRule(13L, 10.0, true, 20.0, true, "0.50");
        when(extraFeeRuleRepository.findById(13L)).thenReturn(Optional.of(current));

        extraFeeRuleService.delete(13L);

        assertThat(current.isCurrent()).isFalse();
        verify(extraFeeRuleRepository).save(current);
    }

    @Test
    void delete_expiredRule_throwsConflict() {
        ExtraFeeRule expired = windRule(13L, 10.0, true, 20.0, true, "0.50");
        expired.expire(Instant.parse("2026-10-01T00:00:00Z"));
        when(extraFeeRuleRepository.findById(13L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> extraFeeRuleService.delete(13L))
                .isInstanceOf(ConflictException.class);
    }

    private void givenCurrentWindRules(VehicleType vehicleType, ExtraFeeRule... rules) {
        when(extraFeeRuleRepository.findAllByTypeAndVehicleTypeAndValidToIsNull(ExtraFeeType.WIND_SPEED, vehicleType))
                .thenReturn(List.of(rules));
    }

    private void returnSavedRule() {
        when(extraFeeRuleRepository.save(any(ExtraFeeRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static ExtraFeeRule windRule(Long id, Double min, boolean minInclusive,
                                         Double max, boolean maxInclusive, String fee) {
        ExtraFeeRule rule = ExtraFeeRule.builder()
                .type(ExtraFeeType.WIND_SPEED)
                .vehicleType(VehicleType.BIKE)
                .minValue(min).minInclusive(minInclusive)
                .maxValue(max).maxInclusive(maxInclusive)
                .fee(new BigDecimal(fee))
                .validFrom(Instant.EPOCH)
                .build();
        ReflectionTestUtils.setField(rule, "id", id);
        return rule;
    }

    private static ExtraFeeRule forbiddenWindRule(Long id, Double min, boolean minInclusive) {
        ExtraFeeRule rule = ExtraFeeRule.builder()
                .type(ExtraFeeType.WIND_SPEED)
                .vehicleType(VehicleType.BIKE)
                .minValue(min).minInclusive(minInclusive)
                .forbidden(true)
                .validFrom(Instant.EPOCH)
                .build();
        ReflectionTestUtils.setField(rule, "id", id);
        return rule;
    }

    private static ExtraFeeRule phenomenonRule(Long id, PhenomenonCategory category) {
        ExtraFeeRule rule = ExtraFeeRule.builder()
                .type(ExtraFeeType.WEATHER_PHENOMENON)
                .vehicleType(VehicleType.BIKE)
                .phenomenonCategory(category)
                .fee(new BigDecimal("1.00"))
                .validFrom(Instant.EPOCH)
                .build();
        ReflectionTestUtils.setField(rule, "id", id);
        return rule;
    }
}