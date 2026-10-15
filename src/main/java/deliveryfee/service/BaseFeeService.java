package deliveryfee.service;

import deliveryfee.dto.BaseFeeRequest;
import deliveryfee.dto.BaseFeeResponse;
import deliveryfee.dto.BaseFeeUpdateRequest;
import deliveryfee.entity.BaseFee;
import deliveryfee.exception.ConflictException;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.repository.BaseFeeRepository;

import java.time.Instant;
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
     * Creates a new base fee valid from now.
     *
     * @param request city, vehicle type and fee
     * @return created base fee
     * @throws ConflictException if a current base fee already exists for this city and vehicle type
     */
    @Transactional
    public BaseFeeResponse create(BaseFeeRequest request) {
        if (baseFeeRepository.existsByCityAndVehicleTypeAndValidToIsNull(request.city(), request.vehicleType())) {
            throw new ConflictException("Base fee for " + request.city() + " and " + request.vehicleType()
                    + " already exists.");
        }
        BaseFee baseFee = baseFeeMapper.toEntity(request, Instant.now());
        return baseFeeMapper.toResponse(baseFeeRepository.save(baseFee));
    }

    /**
     * Changes the amount of a current base fee. The existing fee is expired
     * and a new version with the new amount is created, so the new fee has a new id.
     *
     * @param id id of the current base fee
     * @param request new amount
     * @return new version of the base fee
     * @throws ResourceNotFoundException if there is no base fee with this id
     * @throws ConflictException if the base fee is already expired
     */
    @Transactional
    public BaseFeeResponse update(Long id, BaseFeeUpdateRequest request) {
        BaseFee current = findEntity(id);
        if (!current.isCurrent()) {
            throw new ConflictException("Base fee with id " + id + " is expired and cannot be changed");
        }
        BaseFee next = current.replaceFee(request.fee(), Instant.now());
        baseFeeRepository.save(current);
        return baseFeeMapper.toResponse(baseFeeRepository.save(next));
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