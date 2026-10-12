package com.shruthan.sweet.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.TrafficLight;
import com.shruthan.sweet.model.TrafficLightState;
import com.shruthan.sweet.model.Vehicle;
import com.shruthan.sweet.model.VehicleStatus;

public class SimulationEngine {

    private City city;
    private final SimulationClock clock;
    private final List<SimulationEvent> events = new ArrayList<>();
    private Map<String, VehicleResetState> vehicleResetStates;
    private Map<String, TrafficLightState> trafficLightResetStates;

    public SimulationEngine(City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        this.city = city;
        this.clock = new SimulationClock();
        captureVehicleResetStates();
    }

    public SimulationEngine(City city, SimulationClock clock) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        this.city = city;
        this.clock = clock == null ? new SimulationClock() : clock;
        captureVehicleResetStates();
    }

    public City getCity() {
        return city;
    }

    public void updateCity(City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        this.city = city;
        captureVehicleResetStates();
    }

    public SimulationClock getClock() {
        return clock;
    }

    public List<SimulationEvent> getEvents() {
        return events;
    }

    public void start() {
        clock.start();
    }

    public void pause() {
        clock.pause();
    }

    public void resume() {
        clock.resume();
    }

    public void reset() {
        clock.reset();
        for (Vehicle vehicle : city.getVehicles()) {
            if (vehicle == null) {
                continue;
            }
            VehicleResetState initialState = vehicleResetStates.get(vehicle.getId());
            if (initialState == null) {
                vehicle.setStatus(VehicleStatus.IDLE);
                vehicle.setCurrentIntersection(null);
                continue;
            }
            vehicle.setX(initialState.x());
            vehicle.setY(initialState.y());
            vehicle.setStatus(initialState.status());
            vehicle.setCurrentIntersection(findIntersection(initialState.currentIntersectionId()));
            vehicle.setDestination(findIntersection(initialState.destinationId()));
            vehicle.setRoute(initialState.routeIds().stream()
                    .map(this::findIntersection)
                    .filter(Objects::nonNull)
                    .toList());
        }
        for (Intersection intersection : city.getIntersections()) {
            TrafficLight light = intersection.getTrafficLight();
            if (light != null) {
                TrafficLightState initialState = trafficLightResetStates.get(light.getId());
                if (initialState != null) {
                    light.setState(initialState);
                }
            }
        }
        events.clear();
    }

    public void tick() {
        if (!clock.isRunning()) {
            return;
        }

        advanceOneTick();
    }

    public void step() {
        advanceOneTick();
    }

    private void advanceOneTick() {
        clock.tick();
        updateTrafficLights();
        updateVehicles();
        addEvent(new SimulationEvent("event-" + clock.getCurrentTick(), clock.getCurrentTick(), "tick",
                "Simulation advanced to tick " + clock.getCurrentTick()));
    }

    public void addEvent(SimulationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        events.add(event);
    }

    private void updateTrafficLights() {
        for (Intersection intersection : city.getIntersections()) {
            TrafficLight light = intersection.getTrafficLight();
            if (light != null) {
                light.advance();
            }
        }
    }

    private void updateVehicles() {
        for (Vehicle vehicle : city.getVehicles()) {
            if (vehicle == null) {
                continue;
            }
            if (vehicle.getCurrentIntersection() == null && !vehicle.getRoute().isEmpty()) {
                vehicle.setCurrentIntersection(vehicle.getRoute().remove(0));
                vehicle.setX(vehicle.getCurrentIntersection().getX());
                vehicle.setY(vehicle.getCurrentIntersection().getY());
                vehicle.setStatus(VehicleStatus.MOVING);
                continue;
            }
            vehicle.advanceRoute();
        }
    }

    private void captureVehicleResetStates() {
        Map<String, VehicleResetState> states = new HashMap<>();
        for (Vehicle vehicle : city.getVehicles()) {
            if (vehicle == null) {
                continue;
            }
            states.put(vehicle.getId(), new VehicleResetState(vehicle.getX(), vehicle.getY(), vehicle.getStatus(),
                    idOf(vehicle.getCurrentIntersection()),
                    vehicle.getRoute().stream().map(SimulationEngine::idOf).toList(),
                    idOf(vehicle.getDestination())));
        }
        vehicleResetStates = states;
        Map<String, TrafficLightState> lightStates = new HashMap<>();
        for (Intersection intersection : city.getIntersections()) {
            TrafficLight light = intersection.getTrafficLight();
            if (light != null) {
                lightStates.put(light.getId(), light.getState());
            }
        }
        trafficLightResetStates = lightStates;
    }

    private Intersection findIntersection(String intersectionId) {
        if (intersectionId == null) {
            return null;
        }
        return city.getIntersections().stream()
                .filter(intersection -> intersection != null
                        && Objects.equals(intersection.getId(), intersectionId))
                .findFirst()
                .orElse(null);
    }

    private static String idOf(Intersection intersection) {
        return intersection == null ? null : intersection.getId();
    }

    private record VehicleResetState(
            int x,
            int y,
            VehicleStatus status,
            String currentIntersectionId,
            List<String> routeIds,
            String destinationId) {
    }
}
