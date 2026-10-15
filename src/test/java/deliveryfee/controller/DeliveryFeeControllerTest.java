package deliveryfee.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import deliveryfee.dto.DeliveryFeeResponse;
import deliveryfee.entity.City;
import deliveryfee.entity.VehicleType;
import deliveryfee.exception.ResourceNotFoundException;
import deliveryfee.exception.VehicleUsageForbiddenException;
import deliveryfee.service.DeliveryFeeService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// raise the web layer for this controller only
@WebMvcTest(DeliveryFeeController.class)
class DeliveryFeeControllerTest {

    private static final String URL = "/api/delivery-fee";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryFeeService deliveryFeeService;

    @Test
    void getDeliveryFee_validRequest_returnsFee() throws Exception {
        when(deliveryFeeService.calculate(eq(City.TARTU), eq(VehicleType.BIKE), any(Instant.class)))
                .thenReturn(new DeliveryFeeResponse(City.TARTU, VehicleType.BIKE, new BigDecimal("4.00"), List.of()));

        mockMvc.perform(get(URL).param("city", "TARTU").param("vehicleType", "BIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("TARTU"))
                .andExpect(jsonPath("$.fee").value(4.00))
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    void getDeliveryFee_withDatetime_passesThatMomentToService() throws Exception {
        Instant moment = Instant.parse("2026-10-14T12:00:00Z");
        when(deliveryFeeService.calculate(City.TARTU, VehicleType.BIKE, moment))
                .thenReturn(new DeliveryFeeResponse(City.TARTU, VehicleType.BIKE, new BigDecimal("2.50"), List.of()));

        mockMvc.perform(get(URL)
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", "2026-10-14T15:00:00+03:00"))
                .andExpect(status().isOk());

        verify(deliveryFeeService).calculate(City.TARTU, VehicleType.BIKE, moment);
    }

    @Test
    void getDeliveryFee_forbiddenVehicle_returns422() throws Exception {
        when(deliveryFeeService.calculate(any(), any(), any()))
                .thenThrow(new VehicleUsageForbiddenException());

        mockMvc.perform(get(URL).param("city", "PARNU").param("vehicleType", "BIKE"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Usage of selected vehicle type is forbidden"));
    }

    @Test
    void getDeliveryFee_dataNotFound_returns404() throws Exception {
        when(deliveryFeeService.calculate(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("No weather data for TARTU"));

        mockMvc.perform(get(URL).param("city", "TARTU").param("vehicleType", "BIKE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No weather data for TARTU"));
    }

    @Test
    void getDeliveryFee_unknownCity_returns400WithAllowedValues() throws Exception {
        mockMvc.perform(get(URL).param("city", "BERLIN").param("vehicleType", "BIKE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'BERLIN' for parameter 'city'. Allowed values: [TALLINN, TARTU, PARNU]"));
    }

    @Test
    void getDeliveryFee_missingVehicleType_returns400() throws Exception {
        mockMvc.perform(get(URL).param("city", "TARTU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: vehicleType"));
    }

    @Test
    void getDeliveryFee_invalidDatetime_returns400() throws Exception {
        mockMvc.perform(get(URL)
                        .param("city", "TARTU")
                        .param("vehicleType", "BIKE")
                        .param("datetime", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'yesterday' for parameter 'datetime'"));
    }

    @Test
    void getDeliveryFee_unexpectedError_returns500WithoutDetails() throws Exception {
        when(deliveryFeeService.calculate(any(), any(), any()))
                .thenThrow(new IllegalStateException("database exploded"));

        mockMvc.perform(get(URL).param("city", "TARTU").param("vehicleType", "BIKE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }
}