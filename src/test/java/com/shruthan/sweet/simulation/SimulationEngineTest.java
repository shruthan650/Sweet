package com.shruthan.sweet.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Road;
import com.shruthan.sweet.model.RoadType;
import com.shruthan.sweet.model.TrafficLight;
import com.shruthan.sweet.model.TrafficLightState;
import com.shruthan.sweet.model.Vehicle;
import com.shruthan.sweet.model.VehicleStatus;

class SimulationEngineTest {

    @Test
    void clockStartsTicksAndResetsCleanly() {
        SimulationClock clock = new SimulationClock();

        assertEquals(0L, clock.getCurrentTick());
        assertFalse(clock.isRunning());

        clock.start();
        clock.tick();
        assertTrue(clock.isRunning());
        assertEquals(1L, clock.getCurrentTick());

        clock.pause();
        clock.tick();
        assertEquals(2L, clock.getCurrentTick());

        clock.resume();
        clock.reset();
        assertEquals(0L, clock.getCurrentTick());
        assertFalse(clock.isRunning());
    }

    @Test
    void engineRunsDeterministicTickSequence() {
        City city = new City("city-1", "Downtown", 20, 20);
        Intersection start = new Intersection("i1", 2, 2);
        Intersection end = new Intersection("i2", 8, 2);
        TrafficLight light = new TrafficLight("tl-1", start, TrafficLightState.RED, 5);
        start.setTrafficLight(light);
        city.addIntersection(start);
        city.addIntersection(end);
        city.addRoad(new Road("r1", "Main", start, end, RoadType.STREET));

        Vehicle vehicle = new Vehicle("v1", "Taxi", 2, 2, 30);
        vehicle.setCurrentIntersection(start);
        vehicle.setRoute(List.of(end));
        vehicle.setDestination(end);
        city.addVehicle(vehicle);

        SimulationEngine engine = new SimulationEngine(city);
        engine.start();
        engine.tick();

        assertEquals(1L, engine.getClock().getCurrentTick());
        assertEquals(TrafficLightState.GREEN, light.getState());
        assertEquals(VehicleStatus.ARRIVED, vehicle.getStatus());
        assertEquals(end.getX(), vehicle.getX());
        assertEquals(end.getY(), vehicle.getY());
    }

    @Test
    void engineStopsWhenPaused() {
        City city = new City("city-1", "Downtown", 10, 10);
        Vehicle vehicle = new Vehicle("v1", "Car", 1, 1, 20);
        city.addVehicle(vehicle);

        SimulationEngine engine = new SimulationEngine(city);
        engine.pause();
        engine.tick();

        assertEquals(0L, engine.getClock().getCurrentTick());
        assertEquals(VehicleStatus.IDLE, vehicle.getStatus());
    }

    @Test
    void stepAdvancesExactlyOneTickWhilePaused() {
        City city = new City("city-1", "Downtown", 10, 10);
        Intersection start = new Intersection("i1", 1, 1);
        Intersection end = new Intersection("i2", 4, 1);
        city.addIntersection(start);
        city.addIntersection(end);
        city.addRoad(new Road("r1", "Main", start, end, RoadType.STREET));
        Vehicle vehicle = new Vehicle("v1", "Car", 1, 1, 20);
        vehicle.setCurrentIntersection(start);
        vehicle.setRoute(List.of(end));
        vehicle.setDestination(end);
        city.addVehicle(vehicle);
        SimulationEngine engine = new SimulationEngine(city);

        engine.step();

        assertEquals(1L, engine.getClock().getCurrentTick());
        assertFalse(engine.getClock().isRunning());
        assertEquals(4, vehicle.getX());
        assertEquals(VehicleStatus.ARRIVED, vehicle.getStatus());
    }

    @Test
    void trafficDemoVehiclesKeepFollowingTheirRouteWhileLightsCycle() {
        City city = new City("sweet-traffic-demo-v5", "Sweet Traffic Demo", 10, 10);
        Intersection start = new Intersection("i1", 1, 1);
        Intersection middle = new Intersection("i2", 4, 1);
        Intersection destination = new Intersection("i3", 7, 1);
        TrafficLight light = new TrafficLight("tl-1", start, TrafficLightState.RED, 1);
        start.setTrafficLight(light);
        city.addIntersection(start);
        city.addIntersection(middle);
        city.addIntersection(destination);

        Vehicle vehicle = new Vehicle("v1", "Taxi", start.getX(), start.getY(), 30);
        vehicle.setCurrentIntersection(start);
        vehicle.setDestination(destination);
        vehicle.setRoute(List.of(middle, destination, middle, start));
        city.addVehicle(vehicle);

        SimulationEngine engine = new SimulationEngine(city);
        engine.start();
        engine.tick();
        assertEquals(middle.getX(), vehicle.getX());
        assertEquals(TrafficLightState.GREEN, light.getState());

        engine.tick();
        assertEquals(destination.getX(), vehicle.getX());
        assertEquals(VehicleStatus.ARRIVED, vehicle.getStatus());
        assertEquals(TrafficLightState.YELLOW, light.getState());

        engine.tick();
        engine.tick();
        assertEquals(start.getX(), vehicle.getX());
        assertFalse(vehicle.getRoute().isEmpty(), "the completed sample route should be replenished");
        assertEquals(TrafficLightState.GREEN, light.getState());

        engine.tick();
        assertEquals(middle.getX(), vehicle.getX());
        assertEquals(TrafficLightState.YELLOW, light.getState());
    }
}
