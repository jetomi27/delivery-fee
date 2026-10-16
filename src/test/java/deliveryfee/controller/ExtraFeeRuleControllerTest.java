package deliveryfee.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import deliveryfee.dto.ExtraFeeRuleRequest;
import deliveryfee.dto.ExtraFeeRuleResponse;
import deliveryfee.dto.ExtraFeeRuleUpdateRequest;
import deliveryfee.entity.ExtraFeeType;
import deliveryfee.entity.VehicleType;
import deliveryfee.exception.ConflictException;
import deliveryfee.exception.InvalidRequestException;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.service.ExtraFeeRuleService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExtraFeeRuleController.class)
class ExtraFeeRuleControllerTest {

    private static final String URL = "/api/extra-fee-rules";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtraFeeRuleService extraFeeRuleService;

    @Test
    void getCurrentRules_returnsList() throws Exception {
        when(extraFeeRuleService.getCurrentRules()).thenReturn(List.of(windResponse(13L, "0.50")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(13))
                .andExpect(jsonPath("$[0].type").value("WIND_SPEED"));
    }

    @Test
    void getById_existingId_returnsRule() throws Exception {
        when(extraFeeRuleService.getById(13L)).thenReturn(windResponse(13L, "0.50"));

        mockMvc.perform(get(URL + "/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minValue").value(10.0));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        when(extraFeeRuleService.getById(99L)).thenThrow(new ResourceNotFoundException("Extra fee rule with id 99 not found"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validRule_returns201() throws Exception {
        when(extraFeeRuleService.create(any(ExtraFeeRuleRequest.class))).thenReturn(windResponse(30L, "0.50"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WIND_SPEED", "vehicleType": "SCOOTER",
                                 "minValue": 15, "minInclusive": true, "fee": 0.50, "forbidden": false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30));
    }

    @Test
    void create_phenomenonRuleWithBounds_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WEATHER_PHENOMENON", "vehicleType": "BIKE", "minValue": 5,
                                 "phenomenonCategory": "SNOW", "fee": 1.00, "forbidden": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("phenomenon rules need a category")));
    }

    @Test
    void create_forbiddenRuleWithFee_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WIND_SPEED", "vehicleType": "BIKE", "minValue": 20,
                                 "fee": 1.00, "forbidden": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("must be empty for forbidding rules")));
    }

    @Test
    void create_minGreaterThanMax_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WIND_SPEED", "vehicleType": "BIKE", "minValue": 20,
                                 "maxValue": 10, "fee": 0.50, "forbidden": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("minValue must not be greater than maxValue")));
    }

    @Test
    void create_overlappingRule_returns409() throws Exception {
        when(extraFeeRuleService.create(any(ExtraFeeRuleRequest.class)))
                .thenThrow(new ConflictException("Rule overlaps with current rule with id 13"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WIND_SPEED", "vehicleType": "BIKE", "minValue": 15,
                                 "maxValue": 25, "fee": 0.30, "forbidden": false}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Rule overlaps with current rule with id 13"));
    }

    @Test
    void update_validRequest_returnsNewVersion() throws Exception {
        when(extraFeeRuleService.update(eq(13L), any(ExtraFeeRuleUpdateRequest.class)))
                .thenReturn(windResponse(31L, "0.70"));

        mockMvc.perform(put(URL + "/13")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minValue": 10, "minInclusive": true, "maxValue": 20, "maxInclusive": true,
                                 "fee": 0.70, "forbidden": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.fee").value(0.70));
    }

    @Test
    void update_boundsDoNotFitRuleType_returns400() throws Exception {
        when(extraFeeRuleService.update(eq(20L), any(ExtraFeeRuleUpdateRequest.class)))
                .thenThrow(new InvalidRequestException("Phenomenon rules have no range bounds"));

        mockMvc.perform(put(URL + "/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minValue": 5, "fee": 1.00, "forbidden": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phenomenon rules have no range bounds"));
    }

    @Test
    void delete_currentRule_returns204() throws Exception {
        mockMvc.perform(delete(URL + "/13"))
                .andExpect(status().isNoContent());

        verify(extraFeeRuleService).delete(13L);
    }

    @Test
    void delete_expiredRule_returns409() throws Exception {
        doThrow(new ConflictException("Extra fee rule with id 13 is already expired"))
                .when(extraFeeRuleService).delete(13L);

        mockMvc.perform(delete(URL + "/13"))
                .andExpect(status().isConflict());
    }

    private static ExtraFeeRuleResponse windResponse(Long id, String fee) {
        return new ExtraFeeRuleResponse(id, ExtraFeeType.WIND_SPEED, VehicleType.BIKE,
                10.0, true, 20.0, true, null, new BigDecimal(fee), false, Instant.EPOCH, null);
    }
}