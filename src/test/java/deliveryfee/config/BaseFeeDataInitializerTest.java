package deliveryfee.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.repository.BaseFeeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseFeeDataInitializerTest {

    @Mock
    private BaseFeeRepository baseFeeRepository;

    @InjectMocks
    private BaseFeeDataInitializer initializer;

    @Captor
    private ArgumentCaptor<List<BaseFee>> savedCaptor;

    @Test
    void run_emptyTable_insertsAllDefaultFees() {
        when(baseFeeRepository.count()).thenReturn(0L);

        initializer.run(null);

        verify(baseFeeRepository).saveAll(savedCaptor.capture());
        List<BaseFee> saved = savedCaptor.getValue();
        assertThat(saved).hasSize(9);
        assertThat(saved).allMatch(fee -> fee.getValidTo() == null);

        BaseFee tartuBike = saved.stream()
                .filter(fee -> fee.getCity() == City.TARTU && fee.getVehicleType() == VehicleType.BIKE)
                .findFirst()
                .orElseThrow();
        assertThat(tartuBike.getFee()).isEqualByComparingTo("2.50");
    }

    @Test
    void run_feesAlreadyExist_insertsNothing() {
        when(baseFeeRepository.count()).thenReturn(9L);

        initializer.run(null);

        verify(baseFeeRepository, never()).saveAll(anyList());
    }
}