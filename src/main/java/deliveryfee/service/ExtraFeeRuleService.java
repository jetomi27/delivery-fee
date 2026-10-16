package deliveryfee.service;

import deliveryfee.dto.ExtraFeeRuleResponse;
import deliveryfee.entity.ExtraFeeRule;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.mapper.ExtraFeeRuleMapper;
import deliveryfee.repository.ExtraFeeRuleRepository;
import java.util.List;
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
}