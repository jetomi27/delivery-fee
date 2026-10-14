package deliveryfee.repository;

import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.VehicleType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtraFeeRuleRepository extends JpaRepository<ExtraFeeRule, Long> {

    // returns all extra fee rules for the vehicle type that were valid at the given time
    @Query("""
            SELECT r FROM ExtraFeeRule r
            WHERE r.vehicleType = :vehicleType
              AND r.validFrom <= :time
              AND (r.validTo IS NULL OR r.validTo > :time)
            """)
    List<ExtraFeeRule> findValidAt(@Param("vehicleType") VehicleType vehicleType,
                                   @Param("time") Instant time);
}