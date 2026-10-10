package com.shruthan.sweet.simulation;

import java.util.ArrayList;
import java.util.List;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Vehicle;

public class SimulationEngine {

    private final City city;
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

    public void reset() {
        clock.reset();
        events.clear();
    }

    public void tick() {
        if (!clock.isRunning()) {
            return;
        }
        clock.tick();
        for (Vehicle vehicle : city.getVehicles()) {
            if (vehicle.getStatus() == com.shruthan.sweet.model.VehicleStatus.IDLE) {
                vehicle.setStatus(com.shruthan.sweet.model.VehicleStatus.MOVING);
            }
        }
    }

    public void addEvent(SimulationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        events.add(event);
    }
}
