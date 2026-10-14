package deliveryfee.config;

import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.repository.BaseFeeRepository;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads default regional base fees on first startup
 */
@Slf4j
@Component
public class BaseFeeDataInitializer implements ApplicationRunner {

    private static final Instant DEFAULT_VALID_FROM = Instant.EPOCH;

    private final BaseFeeRepository baseFeeRepository;

    public BaseFeeDataInitializer(BaseFeeRepository baseFeeRepository) {
        this.baseFeeRepository = baseFeeRepository;
    }

    /**
    * Inserts the default base fees if the base fee table is empty.
    *
    * @param args application arguments, not used
    */
    @Override
    public void run(ApplicationArguments args) {
        if (baseFeeRepository.count() > 0) {
            log.info("Base fees already exist, skipping default data");
            return;
        }

        List<BaseFee> defaults = List.of(
                fee(City.TALLINN, VehicleType.CAR, "4.00"),
                fee(City.TALLINN, VehicleType.SCOOTER, "3.50"),
                fee(City.TALLINN, VehicleType.BIKE, "3.00"),
                fee(City.TARTU, VehicleType.CAR, "3.50"),
                fee(City.TARTU, VehicleType.SCOOTER, "3.00"),
                fee(City.TARTU, VehicleType.BIKE, "2.50"),
                fee(City.PARNU, VehicleType.CAR, "3.00"),
                fee(City.PARNU, VehicleType.SCOOTER, "2.50"),
                fee(City.PARNU, VehicleType.BIKE, "2.00"));

        baseFeeRepository.saveAll(defaults);
        log.info("Inserted {} default base fees", defaults.size());
    }

    private BaseFee fee(City city, VehicleType vehicleType, String amount) {
        return BaseFee.builder()
                .city(city)
                .vehicleType(vehicleType)
                .fee(new BigDecimal(amount))
                .validFrom(DEFAULT_VALID_FROM)
                .build();
    }
}