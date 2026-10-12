package com.shruthan.sweet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shruthan.sweet.dto.SimulationStatusDto;
import com.shruthan.sweet.dto.VehicleRouteDto;
import com.shruthan.sweet.service.SimulationService;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/cities/{cityId}/start")
    public ResponseEntity<SimulationStatusDto> start(@PathVariable String cityId) {
        try {
            return ResponseEntity.ok(simulationService.start(cityId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cities/{cityId}/tick")
    public ResponseEntity<SimulationStatusDto> tick(@PathVariable String cityId) {
        try {
            return ResponseEntity.ok(simulationService.tick(cityId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cities/{cityId}/step")
    public ResponseEntity<SimulationStatusDto> step(@PathVariable String cityId) {
        try {
            return ResponseEntity.ok(simulationService.step(cityId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cities/{cityId}/pause")
    public ResponseEntity<SimulationStatusDto> pause(@PathVariable String cityId) {
        try {
            return ResponseEntity.ok(simulationService.pause(cityId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cities/{cityId}/resume")
    public ResponseEntity<SimulationStatusDto> resume(@PathVariable String cityId) {
        try {
            return ResponseEntity.ok(simulationService.resume(cityId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cities/{cityId}/reset")
    public ResponseEntity<SimulationStatusDto> reset(@PathVariable String cityId) {
        try {
            return ResponseEntity.ok(simulationService.reset(cityId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/cities/{cityId}/vehicles/{vehicleId}/destination")
    public ResponseEntity<VehicleRouteDto> assignDestination(
            @PathVariable String cityId,
            @PathVariable String vehicleId,
            @RequestBody DestinationRequest request) {
        if (request == null || request.destinationIntersectionId() == null
                || request.destinationIntersectionId().isBlank()) {
            throw new IllegalArgumentException("Destination intersection id is required");
        }
        return ResponseEntity.ok(simulationService.assignDestination(
                cityId, vehicleId, request.destinationIntersectionId()));
    }

    public record DestinationRequest(String destinationIntersectionId) {
    }
}
