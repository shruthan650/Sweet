package com.shruthan.sweet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shruthan.sweet.dto.SimulationStatusDto;
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
}
