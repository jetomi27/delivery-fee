package deliveryfee.controller;

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

import deliveryfee.dto.BaseFeeRequest;
import deliveryfee.dto.BaseFeeResponse;
import deliveryfee.dto.BaseFeeUpdateRequest;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.exception.ConflictException;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.service.BaseFeeService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BaseFeeController.class)
class BaseFeeControllerTest {

    private static final String URL = "/api/base-fees";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BaseFeeService baseFeeService;

    @Test
    void getCurrentFees_returnsList() throws Exception {
        when(baseFeeService.getCurrentFees()).thenReturn(List.of(response(1L, "2.50")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].city").value("TARTU"));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        when(baseFeeService.getById(99L)).thenThrow(new ResourceNotFoundException("Base fee with id 99 not found"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Base fee with id 99 not found"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(baseFeeService.create(any(BaseFeeRequest.class))).thenReturn(response(10L, "2.80"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "PARNU", "vehicleType": "SCOOTER", "fee": 2.80}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void create_negativeFeeAndMissingCity_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleType": "BIKE", "fee": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("city")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("fee")));
    }

    @Test
    void getById_existingId_returnsFee() throws Exception {
        when(baseFeeService.getById(5L)).thenReturn(response(5L, "2.50"));

        mockMvc.perform(get(URL + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.city").value("TARTU"))
                .andExpect(jsonPath("$.fee").value(2.50));
    }

    @Test
    void create_unknownCity_returns400WithAllowedValues() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "MAARDU", "vehicleType": "BIKE", "fee": 2.50}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'MAARDU' for field 'city'. Allowed values: [TALLINN, TARTU, PARNU]"));
    }

    @Test
    void create_textInsteadOfNumber_returns400WithFieldName() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "TARTU", "vehicleType": "BIKE", "fee": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for field 'fee'"));
    }

    @Test
    void create_objectInsteadOfNumber_returns400WithFieldName() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "TARTU", "vehicleType": "BIKE", "fee": {"amount": 2.5}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing or invalid value for field 'fee'"));
    }

    @Test
    void create_brokenJson_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\": \"TARTU\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or has invalid format"));
    }

    @Test
    void create_emptyBody_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or has invalid format"));
}

    @Test
    void create_currentFeeExists_returns409() throws Exception {
        when(baseFeeService.create(any(BaseFeeRequest.class)))
                .thenThrow(new ConflictException("Base fee for TARTU and BIKE already exists, use PUT to change it"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "TARTU", "vehicleType": "BIKE", "fee": 3.00}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void create_xmlBody_returns415() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<request/>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void update_validRequest_returnsNewVersion() throws Exception {
        when(baseFeeService.update(eq(6L), any(BaseFeeUpdateRequest.class))).thenReturn(response(33L, "3.20"));

        mockMvc.perform(put(URL + "/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fee": 3.20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(33))
                .andExpect(jsonPath("$.fee").value(3.20));
    }

    @Test
    void delete_currentFee_returns204() throws Exception {
        mockMvc.perform(delete(URL + "/7"))
                .andExpect(status().isNoContent());

        verify(baseFeeService).delete(7L);
    }

    @Test
    void delete_expiredFee_returns409() throws Exception {
        doThrow(new ConflictException("Base fee with id 7 is already expired"))
                .when(baseFeeService).delete(7L);

        mockMvc.perform(delete(URL + "/7"))
                .andExpect(status().isConflict());
    }

    private static BaseFeeResponse response(Long id, String fee) {
        return new BaseFeeResponse(id, City.TARTU, VehicleType.BIKE, new BigDecimal(fee), Instant.EPOCH, null);
    }
}