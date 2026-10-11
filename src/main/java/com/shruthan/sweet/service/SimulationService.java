package com.shruthan.sweet.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.shruthan.sweet.dto.SimulationStatusDto;
import com.shruthan.sweet.model.City;
import com.shruthan.sweet.simulation.SimulationEngine;

@Service
public class SimulationService {

    private final CityService cityService;
    private final Map<String, SimulationEngine> engines = new ConcurrentHashMap<>();

    public SimulationService(CityService cityService) {
        this.cityService = cityService;
    }

    public SimulationStatusDto start(String cityId) {
        City city = loadCity(cityId);
        SimulationEngine engine = new SimulationEngine(city);
        engine.start();
        engines.put(cityId, engine);
        return toStatus(cityId, engine);
    }

    public SimulationStatusDto tick(String cityId) {
        SimulationEngine engine = getOrCreate(cityId);
        engine.tick();
        return toStatus(cityId, engine);
    }

    public SimulationStatusDto pause(String cityId) {
        SimulationEngine engine = getOrCreate(cityId);
        engine.pause();
        return toStatus(cityId, engine);
    }

    public SimulationStatusDto resume(String cityId) {
        SimulationEngine engine = getOrCreate(cityId);
        engine.resume();
        return toStatus(cityId, engine);
    }

    public SimulationStatusDto reset(String cityId) {
        SimulationEngine engine = getOrCreate(cityId);
        engine.reset();
        return toStatus(cityId, engine);
    }

    private City loadCity(String cityId) {
        Optional<City> city = cityService.findById(cityId);
        if (city.isEmpty()) {
            throw new IllegalArgumentException("City not found: " + cityId);
        }
        return city.get();
    }

    private SimulationEngine getOrCreate(String cityId) {
        return engines.computeIfAbsent(cityId, ignored -> new SimulationEngine(loadCity(cityId)));
    }

    private SimulationStatusDto toStatus(String cityId, SimulationEngine engine) {
        List<String> eventIds = new ArrayList<>();
        engine.getEvents().forEach(event -> eventIds.add(event.getId()));
        return new SimulationStatusDto(cityId, engine.getClock().getCurrentTick(), engine.getClock().isRunning(), eventIds);
    }
}
