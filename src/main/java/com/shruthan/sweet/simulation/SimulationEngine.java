package com.shruthan.sweet.simulation;

import java.util.ArrayList;
import java.util.List;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.TrafficLight;
import com.shruthan.sweet.model.Vehicle;
import com.shruthan.sweet.model.VehicleStatus;

public class SimulationEngine {

    private City city;
    private final SimulationClock clock;
    private final List<SimulationEvent> events = new ArrayList<>();

    public SimulationEngine(City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        this.city = city;
        this.clock = new SimulationClock();
    }

    public SimulationEngine(City city, SimulationClock clock) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        this.city = city;
        this.clock = clock == null ? new SimulationClock() : clock;
    }

    public City getCity() {
        return city;
    }

    public void updateCity(City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        this.city = city;
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
            vehicle.setStatus(VehicleStatus.IDLE);
            vehicle.setCurrentIntersection(null);
        }
        events.clear();
    }

    public void tick() {
        if (!clock.isRunning()) {
            return;
        }

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
}
