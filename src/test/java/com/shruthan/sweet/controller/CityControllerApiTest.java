package com.shruthan.sweet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.service.CityService;
import com.shruthan.sweet.service.SimulationService;

@WebMvcTest(CityController.class)
@Import(ApiExceptionHandler.class)
class CityControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private SimulationService simulationService;

    @Test
    void getCityReturnsThePersistedCity() throws Exception {
        when(cityService.findById("city-1"))
                .thenReturn(Optional.of(new City("city-1", "Harbor", 12, 8)));

        mockMvc.perform(get("/api/cities/city-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("city-1"))
                .andExpect(jsonPath("$.name").value("Harbor"))
                .andExpect(jsonPath("$.width").value(12))
                .andExpect(jsonPath("$.height").value(8));
    }

    @Test
    void getMissingCityReturnsNotFound() throws Exception {
        when(cityService.findById("missing-city")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cities/missing-city"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCityReturnsCreatedStatusAndCityRepresentation() throws Exception {
        when(cityService.create(any(City.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"city-1","name":"Harbor","width":12,"height":8}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("city-1"))
                .andExpect(jsonPath("$.name").value("Harbor"));
    }
}
