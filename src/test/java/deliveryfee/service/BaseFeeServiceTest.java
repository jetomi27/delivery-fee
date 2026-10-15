package deliveryfee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import deliveryfee.dto.BaseFeeRequest;
import deliveryfee.dto.BaseFeeResponse;
import deliveryfee.dto.BaseFeeUpdateRequest;
import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.exception.ConflictException;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.mapper.BaseFeeMapper;
import deliveryfee.repository.BaseFeeRepository;
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

@ExtendWith(MockitoExtension.class)
class BaseFeeServiceTest {

    @Mock
    private BaseFeeRepository baseFeeRepository;

    @Spy
    private BaseFeeMapper baseFeeMapper = Mappers.getMapper(BaseFeeMapper.class);

    @InjectMocks
    private BaseFeeService baseFeeService;

    @Captor
    private ArgumentCaptor<BaseFee> savedCaptor;

    @Test
    void getCurrentFees_returnsMappedFees() {
        when(baseFeeRepository.findAllByValidToIsNullOrderByCityAscVehicleTypeAsc())
                .thenReturn(List.of(fee(City.TARTU, VehicleType.BIKE, "2.50")));

        List<BaseFeeResponse> result = baseFeeService.getCurrentFees();

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.city()).isEqualTo(City.TARTU);
            assertThat(response.fee()).isEqualByComparingTo("2.50");
        });
    }

    @Test
    void getById_unknownId_throwsNotFound() {
        when(baseFeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baseFeeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_noCurrentFee_savesNewFee() {
        when(baseFeeRepository.existsByCityAndVehicleTypeAndValidToIsNull(City.PARNU, VehicleType.SCOOTER))
                .thenReturn(false);
        returnSavedFee();
        Instant before = Instant.now();

        BaseFeeResponse result = baseFeeService.create(
                new BaseFeeRequest(City.PARNU, VehicleType.SCOOTER, new BigDecimal("2.80")));

        verify(baseFeeRepository).save(savedCaptor.capture());
        BaseFee saved = savedCaptor.getValue();
        assertThat(saved.getCity()).isEqualTo(City.PARNU);
        assertThat(saved.getFee()).isEqualByComparingTo("2.80");
        assertThat(saved.getValidFrom()).isBetween(before, Instant.now());
        assertThat(saved.getValidTo()).isNull();
        assertThat(result.fee()).isEqualByComparingTo("2.80");
    }

    @Test
    void create_currentFeeExists_throwsConflict() {
        when(baseFeeRepository.existsByCityAndVehicleTypeAndValidToIsNull(City.TARTU, VehicleType.BIKE))
                .thenReturn(true);

        assertThatThrownBy(() -> baseFeeService.create(
                new BaseFeeRequest(City.TARTU, VehicleType.BIKE, new BigDecimal("3.00"))))
                .isInstanceOf(ConflictException.class);
        verify(baseFeeRepository, never()).save(any());
    }

    @Test
    void getById_existingId_returnsMappedFee() {
        when(baseFeeRepository.findById(5L)).thenReturn(Optional.of(fee(City.TARTU, VehicleType.BIKE, "2.50")));

        BaseFeeResponse result = baseFeeService.getById(5L);

        assertThat(result.city()).isEqualTo(City.TARTU);
        assertThat(result.fee()).isEqualByComparingTo("2.50");
    }

    @Test
    void update_currentFee_expiresOldAndSavesNewVersion() {
        BaseFee current = fee(City.TARTU, VehicleType.BIKE, "2.50");
        when(baseFeeRepository.findById(6L)).thenReturn(Optional.of(current));
        returnSavedFee();

        BaseFeeResponse result = baseFeeService.update(6L, new BaseFeeUpdateRequest(new BigDecimal("3.20")));

        verify(baseFeeRepository, times(2)).save(savedCaptor.capture());
        BaseFee expired = savedCaptor.getAllValues().get(0);
        BaseFee next = savedCaptor.getAllValues().get(1);
        assertThat(expired.isCurrent()).isFalse();
        assertThat(next.isCurrent()).isTrue();
        assertThat(next.getValidFrom()).isEqualTo(expired.getValidTo());
        assertThat(next.getCity()).isEqualTo(City.TARTU);
        assertThat(result.fee()).isEqualByComparingTo("3.20");
    }

    @Test
    void update_expiredFee_throwsConflict() {
        BaseFee expired = fee(City.TARTU, VehicleType.BIKE, "2.50");
        expired.expire(Instant.parse("2026-10-01T00:00:00Z"));
        when(baseFeeRepository.findById(6L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> baseFeeService.update(6L, new BaseFeeUpdateRequest(new BigDecimal("3.20"))))
                .isInstanceOf(ConflictException.class);
        verify(baseFeeRepository, never()).save(any());
    }

    @Test
    void delete_currentFee_expiresIt() {
        BaseFee current = fee(City.PARNU, VehicleType.SCOOTER, "2.50");
        when(baseFeeRepository.findById(7L)).thenReturn(Optional.of(current));

        baseFeeService.delete(7L);

        assertThat(current.isCurrent()).isFalse();
        verify(baseFeeRepository).save(current);
    }

    @Test
    void delete_expiredFee_throwsConflictAndKeepsValidTo() {
        Instant originalValidTo = Instant.parse("2026-10-01T00:00:00Z");
        BaseFee expired = fee(City.PARNU, VehicleType.SCOOTER, "2.50");
        expired.expire(originalValidTo);
        when(baseFeeRepository.findById(7L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> baseFeeService.delete(7L))
                .isInstanceOf(ConflictException.class);
        assertThat(expired.getValidTo()).isEqualTo(originalValidTo);
    }

    private void returnSavedFee() {
        when(baseFeeRepository.save(any(BaseFee.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static BaseFee fee(City city, VehicleType vehicleType, String amount) {
        return BaseFee.builder()
                .city(city)
                .vehicleType(vehicleType)
                .fee(new BigDecimal(amount))
                .validFrom(Instant.EPOCH)
                .build();
    }
}