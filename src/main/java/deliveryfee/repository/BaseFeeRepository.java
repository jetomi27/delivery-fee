package deliveryfee.repository;

import deliveryfee.entity.BaseFee;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BaseFeeRepository extends JpaRepository<BaseFee, Long> {

    // returns the base fee for the city and vehicle type that was valid at the given time
    @Query("""
            SELECT b FROM BaseFee b
            WHERE b.city = :city
              AND b.vehicleType = :vehicleType
              AND b.validFrom <= :time
              AND (b.validTo IS NULL OR b.validTo > :time)
            """)
    Optional<BaseFee> findValidAt(@Param("city") City city,
                                  @Param("vehicleType") VehicleType vehicleType,
                                  @Param("time") Instant time);
                                  
    // returns all base fees that are currently valid
    List<BaseFee> findAllByValidToIsNullOrderByCityAscVehicleTypeAsc();
}