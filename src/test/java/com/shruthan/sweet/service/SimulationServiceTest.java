package com.shruthan.sweet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.shruthan.sweet.dto.VehicleRouteDto;
import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Road;
import com.shruthan.sweet.model.RoadDirection;
import com.shruthan.sweet.model.RoadType;
import com.shruthan.sweet.model.Vehicle;
import com.shruthan.sweet.model.VehicleStatus;
import com.shruthan.sweet.repository.CityRepository;

class SimulationServiceTest {

    @Test
    void assignDestinationPersistsComputedRouteWithoutStartingIntersection() {
        City city = cityWithRoad(true);
        InMemoryCityRepository repository = new InMemoryCityRepository(city);
        SimulationService service = new SimulationService(new CityService(repository));

        VehicleRouteDto route = service.assignDestination("city-1", "vehicle-1", "intersection-2");

        assertEquals(new VehicleRouteDto("vehicle-1", "intersection-2", List.of("intersection-2")), route);
        Vehicle persistedVehicle = repository.city.getVehicles().getFirst();
        assertEquals("intersection-1", persistedVehicle.getCurrentIntersection().getId());
        assertEquals(List.of("intersection-2"),
                persistedVehicle.getRoute().stream().map(Intersection::getId).toList());
        assertEquals("intersection-2", persistedVehicle.getDestination().getId());
        assertEquals(VehicleStatus.MOVING, persistedVehicle.getStatus());
    }

    @Test
    void assignDestinationRejectsUnreachableDestinationWithoutChangingVehicle() {
        City city = cityWithRoad(false);
        SimulationService service = new SimulationService(new CityService(new InMemoryCityRepository(city)));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.assignDestination("city-1", "vehicle-1", "intersection-2"));

        assertEquals("No route connects the vehicle to the destination", failure.getMessage());
        assertNull(city.getVehicles().getFirst().getDestination());
        assertEquals(VehicleStatus.IDLE, city.getVehicles().getFirst().getStatus());
    }

    @Test
    void assignDestinationRequiresVehicleToBeAtAnIntersection() {
        City city = cityWithRoad(true);
        city.getVehicles().getFirst().setX(4);
        city.getVehicles().getFirst().setY(4);
        SimulationService service = new SimulationService(new CityService(new InMemoryCityRepository(city)));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.assignDestination("city-1", "vehicle-1", "intersection-2"));

        assertEquals("Vehicle must be positioned at an intersection before assigning a destination",
                failure.getMessage());
        assertEquals("intersection-1", city.getVehicles().getFirst().getCurrentIntersection().getId());
    }

    @Test
    void assignDestinationDoesNotRouteAgainstOneWayRoad() {
        City city = cityWithRoad(false);
        Intersection start = city.getIntersections().get(0);
        Intersection destination = city.getIntersections().get(1);
        city.setRoads(List.of(new Road("road-1", "One Way",
                new Intersection(start.getId(), start.getX(), start.getY()),
                new Intersection(destination.getId(), destination.getX(), destination.getY()),
                RoadType.STREET, 4, false, RoadDirection.HORIZONTAL)));
        Vehicle vehicle = city.getVehicles().getFirst();
        vehicle.setX(destination.getX());
        vehicle.setY(destination.getY());
        vehicle.setCurrentIntersection(new Intersection(destination.getId(), destination.getX(), destination.getY()));
        SimulationService service = new SimulationService(new CityService(new InMemoryCityRepository(city)));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.assignDestination("city-1", "vehicle-1", start.getId()));

        assertEquals("No route connects the vehicle to the destination", failure.getMessage());
        assertNull(vehicle.getDestination());
    }

    private static City cityWithRoad(boolean connected) {
        City city = new City("city-1", "Test City", 10, 10);
        Intersection start = new Intersection("intersection-1", 1, 1);
        Intersection destination = new Intersection("intersection-2", 5, 1);
        city.setIntersections(List.of(start, destination));
        if (connected) {
            city.setRoads(List.of(new Road("road-1", "Main Road",
                    new Intersection("intersection-1", 1, 1),
                    new Intersection("intersection-2", 5, 1),
                    RoadType.STREET, 4, true, RoadDirection.HORIZONTAL)));
        }
        Vehicle vehicle = new Vehicle("vehicle-1", "Car", 1, 1, 30);
        vehicle.setCurrentIntersection(new Intersection("intersection-1", 1, 1));
        city.setVehicles(List.of(vehicle));
        return city;
    }

    private static final class InMemoryCityRepository implements CityRepository {

        private City city;

        private InMemoryCityRepository(City city) {
            this.city = city;
        }

        @Override
        public List<City> findAll() {
            return List.of(city);
        }

        @Override
        public Optional<City> findById(String id) {
            return city.getId().equals(id) ? Optional.of(city) : Optional.empty();
        }

        @Override
        public City create(City value) {
            city = value;
            return city;
        }

        @Override
        public City update(String id, City value) {
            city = value;
            return city;
        }

        @Override
        public void deleteById(String id) {
            if (city.getId().equals(id)) {
                city = null;
            }
        }
    }
}
