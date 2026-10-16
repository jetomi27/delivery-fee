package deliveryfee.service;

import deliveryfee.dto.ExtraFeeRuleRequest;
import deliveryfee.dto.ExtraFeeRuleResponse;
import deliveryfee.dto.ExtraFeeRuleUpdateRequest;
import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.exception.ConflictException;
import deliveryfee.exception.InvalidRequestException;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.mapper.ExtraFeeRuleMapper;
import deliveryfee.repository.ExtraFeeRuleRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages weather extra fee rules. Changes never overwrite existing rules 
*/
@Service
public class ExtraFeeRuleService {

    private final ExtraFeeRuleRepository extraFeeRuleRepository;
    private final ExtraFeeRuleMapper extraFeeRuleMapper;

    public ExtraFeeRuleService(ExtraFeeRuleRepository extraFeeRuleRepository,
                               ExtraFeeRuleMapper extraFeeRuleMapper) {
        this.extraFeeRuleRepository = extraFeeRuleRepository;
        this.extraFeeRuleMapper = extraFeeRuleMapper;
    }

    /**
     * Returns all currently valid extra fee rules.
     *
     * @return current rules ordered by vehicle type and rule type
     */
    @Transactional(readOnly = true)
    public List<ExtraFeeRuleResponse> getCurrentRules() {
        return extraFeeRuleMapper.toResponses(
                extraFeeRuleRepository.findAllByValidToIsNullOrderByVehicleTypeAscTypeAsc());
    }

    /**
     * Returns an extra fee rule by id, including expired versions.
     *
     * @param id rule id
     * @return extra fee rule
     * @throws ResourceNotFoundException if there is no rule with this id
     */
    @Transactional(readOnly = true)
    public ExtraFeeRuleResponse getById(Long id) {
        return extraFeeRuleMapper.toResponse(findEntity(id));
    }

    private ExtraFeeRule findEntity(Long id) {
        return extraFeeRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Extra fee rule with id " + id + " not found"));
    }

    /**
     * Creates a new extra fee rule valid from now.
     *
     * @param request rule parameters
     * @return created rule
     * @throws ConflictException if the rule overlaps with a current rule of the same type and vehicle type
     */
    @Transactional
    public ExtraFeeRuleResponse create(ExtraFeeRuleRequest request) {
        ExtraFeeRule rule = extraFeeRuleMapper.toEntity(request, Instant.now());
        checkNoOverlap(rule, null);
        return extraFeeRuleMapper.toResponse(extraFeeRuleRepository.save(rule));
    }

    /**
     * Changes a current extra fee rule. The existing rule is expired and a new version
     * with the new values is created, so the new rule has a new id.
     *
     * @param id id of the current rule
     * @param request new bounds, fee and forbidden flag
     * @return new version of the rule
     * @throws ResourceNotFoundException if there is no rule with this id
     * @throws ConflictException if the rule is expired or the new version overlaps with another current rule
     * @throws InvalidRequestException if the bounds do not fit the rule type
     */
    @Transactional
    public ExtraFeeRuleResponse update(Long id, ExtraFeeRuleUpdateRequest request) {
        ExtraFeeRule current = findEntity(id);
        if (!current.isCurrent()) {
            throw new ConflictException("Extra fee rule with id " + id + " is expired and cannot be changed");
        }
        checkBoundsMatchType(current.getType(), request);

        Instant now = Instant.now();
        ExtraFeeRule next = ExtraFeeRule.builder()
                .type(current.getType())
                .vehicleType(current.getVehicleType())
                .phenomenonCategory(current.getPhenomenonCategory())
                .minValue(request.minValue())
                .minInclusive(request.minInclusive())
                .maxValue(request.maxValue())
                .maxInclusive(request.maxInclusive())
                .fee(request.fee())
                .forbidden(request.forbidden())
                .validFrom(now)
                .build();
        checkNoOverlap(next, id);

        current.expire(now);
        extraFeeRuleRepository.save(current);
        return extraFeeRuleMapper.toResponse(extraFeeRuleRepository.save(next));
    }

    /**
     * Ends the validity of a current extra fee rule.
     *
     * @param id id of the current rule
     * @throws ResourceNotFoundException if there is no rule with this id
     * @throws ConflictException if the rule is already expired
     */
    @Transactional
    public void delete(Long id) {
        ExtraFeeRule current = findEntity(id);
        if (!current.isCurrent()) {
            throw new ConflictException("Extra fee rule with id " + id + " is already expired");
        }
        current.expire(Instant.now());
        extraFeeRuleRepository.save(current);
    }

    private void checkBoundsMatchType(ExtraFeeType type, ExtraFeeRuleUpdateRequest request) {
        boolean hasBounds = request.minValue() != null || request.maxValue() != null;
        if (type == ExtraFeeType.WEATHER_PHENOMENON && hasBounds) {
            throw new InvalidRequestException("Phenomenon rules have no range bounds");
        }
        if (type != ExtraFeeType.WEATHER_PHENOMENON && !hasBounds) {
            throw new InvalidRequestException("Temperature and wind rules need at least one range bound");
        }
    }

    private void checkNoOverlap(ExtraFeeRule rule, Long replacedRuleId) {
        extraFeeRuleRepository.findAllByTypeAndVehicleTypeAndValidToIsNull(rule.getType(), rule.getVehicleType())
                .stream()
                .filter(existing -> !Objects.equals(existing.getId(), replacedRuleId))
                .filter(rule::overlaps)
                .findFirst()
                .ifPresent(existing -> {
                    throw new ConflictException("Rule overlaps with current rule with id " + existing.getId());
            });
    }
}