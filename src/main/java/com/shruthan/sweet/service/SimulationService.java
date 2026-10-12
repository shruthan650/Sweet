package com.shruthan.sweet.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.shruthan.sweet.algorithm.PathFinder;
import com.shruthan.sweet.dto.SimulationStatusDto;
import com.shruthan.sweet.dto.VehicleRouteDto;
import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Vehicle;
import com.shruthan.sweet.model.VehicleStatus;
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
        long previousTick = engine.getClock().getCurrentTick();
        engine.tick();
        if (engine.getClock().getCurrentTick() != previousTick) {
            cityService.update(cityId, engine.getCity());
        }
        return toStatus(cityId, engine);
    }

    public void updateCity(City city) {
        if (city == null || city.getId() == null || city.getId().isBlank()) {
            throw new IllegalArgumentException("City with an id is required");
        }
        SimulationEngine engine = engines.get(city.getId());
        if (engine != null) {
            engine.updateCity(city);
        }
    }

    public void removeCity(String cityId) {
        if (cityId == null || cityId.isBlank()) {
            throw new IllegalArgumentException("City id is required");
        }
        engines.remove(cityId);
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

    public VehicleRouteDto assignDestination(String cityId, String vehicleId, String destinationIntersectionId) {
        SimulationEngine engine = engines.get(cityId);
        City city = engine == null ? loadCity(cityId) : engine.getCity();
        Vehicle vehicle = city.getVehicles().stream()
                .filter(candidate -> candidate != null && Objects.equals(candidate.getId(), vehicleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        Intersection destination = findIntersection(city, destinationIntersectionId);
        Intersection vehiclePosition = vehicle.getCurrentIntersection();
        Intersection start = vehiclePosition != null
                && vehiclePosition.getX() == vehicle.getX()
                && vehiclePosition.getY() == vehicle.getY()
                        ? findIntersectionOrNull(city, vehiclePosition.getId())
                        : null;
        if (start == null) {
            start = city.getIntersections().stream()
                    .filter(intersection -> intersection.getX() == vehicle.getX()
                            && intersection.getY() == vehicle.getY())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Vehicle must be positioned at an intersection before assigning a destination"));
        }
        List<Intersection> path = PathFinder.findWeightedPath(city, start, destination);
        if (path.isEmpty()) {
            throw new IllegalArgumentException("No route connects the vehicle to the destination");
        }
        vehicle.setCurrentIntersection(start);
        vehicle.setDestination(destination);
        vehicle.setRoute(path.subList(1, path.size()));
        vehicle.setStatus(start.getId().equals(destination.getId()) ? VehicleStatus.ARRIVED : VehicleStatus.MOVING);
        cityService.update(cityId, city);
        List<String> routeIds = vehicle.getRoute().stream()
                .filter(Objects::nonNull)
                .map(intersection -> intersection.getId())
                .collect(Collectors.toUnmodifiableList());
        return new VehicleRouteDto(vehicle.getId(), destination.getId(), routeIds);
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

    private static Intersection findIntersection(City city, String intersectionId) {
        Intersection intersection = findIntersectionOrNull(city, intersectionId);
        if (intersection == null) {
            throw new IllegalArgumentException("Destination intersection not found: " + intersectionId);
        }
        return intersection;
    }

    private static Intersection findIntersectionOrNull(City city, String intersectionId) {
        return city.getIntersections().stream()
                .filter(intersection -> intersection != null && Objects.equals(intersection.getId(), intersectionId))
                .findFirst()
                .orElse(null);
    }

    private SimulationStatusDto toStatus(String cityId, SimulationEngine engine) {
        List<String> eventIds = new ArrayList<>();
        engine.getEvents().forEach(event -> eventIds.add(event.getId()));
        return new SimulationStatusDto(cityId, engine.getClock().getCurrentTick(), engine.getClock().isRunning(), eventIds);
    }
}
