package com.shruthan.sweet.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.shruthan.sweet.dto.SimulationStatusDto;
import com.shruthan.sweet.dto.VehicleRouteDto;
import com.shruthan.sweet.service.SimulationService;

@WebMvcTest(SimulationController.class)
@Import(ApiExceptionHandler.class)
class SimulationControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimulationService simulationService;

    @Test
    void stepReturnsTheUpdatedSimulationStatus() throws Exception {
        when(simulationService.step("city-1"))
                .thenReturn(new SimulationStatusDto("city-1", 7, false, List.of("event-7")));

        mockMvc.perform(post("/api/simulation/cities/city-1/step"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId").value("city-1"))
                .andExpect(jsonPath("$.tick").value(7))
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.events[0]").value("event-7"));
    }

    @Test
    void startingUnknownCityReturnsNotFound() throws Exception {
        when(simulationService.start("missing-city"))
                .thenThrow(new IllegalArgumentException("City not found: missing-city"));

        mockMvc.perform(post("/api/simulation/cities/missing-city/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void assigningDestinationReturnsTheComputedRoute() throws Exception {
        when(simulationService.assignDestination("city-1", "vehicle-1", "intersection-3"))
                .thenReturn(new VehicleRouteDto("vehicle-1", "intersection-3",
                        List.of("intersection-2", "intersection-3")));

        mockMvc.perform(put("/api/simulation/cities/city-1/vehicles/vehicle-1/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationIntersectionId":"intersection-3"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value("vehicle-1"))
                .andExpect(jsonPath("$.destinationIntersectionId").value("intersection-3"))
                .andExpect(jsonPath("$.route[0]").value("intersection-2"))
                .andExpect(jsonPath("$.route[1]").value("intersection-3"));
    }

    @Test
    void assigningDestinationWithoutAnIntersectionReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/simulation/cities/city-1/vehicles/vehicle-1/destination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationIntersectionId":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Destination intersection id is required"));
    }
}
