package deliveryfee.service;

import deliveryfee.dto.BaseFeeResponse;
import deliveryfee.entity.BaseFee;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.repository.BaseFeeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import deliveryfee.mapper.BaseFeeMapper;

/**
 * Manages regional base fees. Changes never overwrite existing fees
 */
@Service
public class BaseFeeService {

    private final BaseFeeRepository baseFeeRepository;
    private final BaseFeeMapper baseFeeMapper;

    public BaseFeeService(BaseFeeRepository baseFeeRepository, BaseFeeMapper baseFeeMapper) {
        this.baseFeeRepository = baseFeeRepository;
        this.baseFeeMapper = baseFeeMapper;
    }

    /**
     * Returns all currently valid base fees.
     *
     * @return current base fees ordered by city and vehicle type
     */
    @Transactional(readOnly = true)
    public List<BaseFeeResponse> getCurrentFees() {
        return baseFeeMapper.toResponses(
                baseFeeRepository.findAllByValidToIsNullOrderByCityAscVehicleTypeAsc());
    }

    /**
     * Returns a base fee by id, including expired versions.
     *
     * @param id base fee id
     * @return base fee
     * @throws ResourceNotFoundException if there is no base fee with this id
     */
    @Transactional(readOnly = true)
    public BaseFeeResponse getById(Long id) {
        return baseFeeMapper.toResponse(findEntity(id));
    }

    private BaseFee findEntity(Long id) {
        return baseFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base fee with id " + id + " not found"));
    }
}