package com.shruthan.sweet.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void cityRequiresPositiveDimensions() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new City("city-1", "Sweet", 0, 10));
        assertEquals("City width must be positive", exception.getMessage());
    }

    @Test
    void roadBuildsMinimalConnectivity() {
        Intersection start = new Intersection("i1", 0, 0);
        Intersection end = new Intersection("i2", 4, 0);
        Road road = new Road("r1", "Main Street", start, end, RoadType.STREET);

        assertEquals(RoadDirection.HORIZONTAL, road.getDirection());
        assertEquals(4, road.getLength());
        assertTrue(road.connects(start, end));
    }

    @Test
    void trafficLightsFollowValidTransitions() {
        TrafficLight light = new TrafficLight("tl-1", new Intersection("i1", 1, 1), TrafficLightState.RED, 15);

        assertEquals(TrafficLightState.RED, light.getState());
        light.advance();
        assertEquals(TrafficLightState.GREEN, light.getState());
        assertTrue(light.canAdvanceTo(TrafficLightState.YELLOW));
        assertFalse(light.canAdvanceTo(TrafficLightState.RED));
    }

    @Test
    void buildingsCanOverlapAndVehiclesCanBeTracked() {
        Building first = new Building("b1", "Tower A", 0, 0, 10, 10, BuildingType.RESIDENTIAL);
        Building second = new Building("b2", "Tower B", 5, 5, 10, 10, BuildingType.COMMERCIAL);
        Vehicle vehicle = new Vehicle("v1", "Taxi", 3, 3, 35);
        vehicle.setStatus(VehicleStatus.MOVING);

        assertTrue(first.overlaps(second));
        assertEquals(VehicleStatus.MOVING, vehicle.getStatus());
        assertNotNull(List.of(first, second));
    }
}
