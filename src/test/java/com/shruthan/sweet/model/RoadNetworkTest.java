package com.shruthan.sweet.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoadNetworkTest {

    @Test
    void validConnectedCityPassesValidation() {
        City city = new City("city-1", "Downtown", 20, 20);

        Intersection a = new Intersection("i1", 2, 2);
        Intersection b = new Intersection("i2", 8, 2);
        Intersection c = new Intersection("i3", 8, 10);
        city.addIntersection(a);
        city.addIntersection(b);
        city.addIntersection(c);
        city.addRoad(new Road("r1", "Main", a, b, RoadType.STREET));
        city.addRoad(new Road("r2", "Cross", b, c, RoadType.AVENUE));

        assertDoesNotThrow(city::validateRoadNetwork);
    }

    @Test
    void outOfBoundsIntersectionIsRejected() {
        City city = new City("city-1", "Downtown", 10, 10);
        Intersection out = new Intersection("bad", 12, 2);

        assertThrows(IllegalArgumentException.class, () -> city.addIntersection(out));
    }

    @Test
    void duplicateRoadSegmentsAreRejected() {
        City city = new City("city-1", "Downtown", 20, 20);
        Intersection a = new Intersection("i1", 1, 1);
        Intersection b = new Intersection("i2", 5, 1);

        city.addIntersection(a);
        city.addIntersection(b);
        city.addRoad(new Road("r1", "Main", a, b, RoadType.STREET));

        assertThrows(IllegalArgumentException.class, () -> city.addRoad(new Road("r2", "Duplicate", a, b, RoadType.STREET)));
    }

    @Test
    void disconnectedRoadNetworkIsRejected() {
        City city = new City("city-1", "Downtown", 20, 20);
        Intersection a = new Intersection("i1", 2, 2);
        Intersection b = new Intersection("i2", 6, 2);
        Intersection c = new Intersection("i3", 12, 12);
        Intersection d = new Intersection("i4", 16, 12);

        city.addIntersection(a);
        city.addIntersection(b);
        city.addIntersection(c);
        city.addIntersection(d);
        city.addRoad(new Road("r1", "Main", a, b, RoadType.STREET));
        city.addRoad(new Road("r2", "Other", c, d, RoadType.AVENUE));

        assertThrows(IllegalArgumentException.class, city::validateRoadNetwork);
    }

    @Test
    void buildingOutsideGridIsRejected() {
        City city = new City("city-1", "Downtown", 10, 10);
        Building building = new Building("b1", "Hotel", 9, 9, 4, 4, BuildingType.RESIDENTIAL);

        assertThrows(IllegalArgumentException.class, () -> city.addBuilding(building));
    }

    @Test
    void overlappingBuildingsAreRejected() {
        City city = new City("city-1", "Downtown", 20, 20);
        city.addBuilding(new Building("b1", "Tower A", 0, 0, 5, 5, BuildingType.RESIDENTIAL));

        assertThrows(IllegalArgumentException.class,
                () -> city.addBuilding(new Building("b2", "Tower B", 3, 3, 5, 5, BuildingType.COMMERCIAL)));
    }

    @Test
    void cityGridContainsCoordinatesAndRegions() {
        City city = new City("city-1", "Downtown", 12, 8);
        assertThrows(IllegalArgumentException.class, () -> new City("city-2", "Bad", 0, 8));
        assertThrows(IllegalArgumentException.class, () -> new City("city-3", "Bad", 12, 0));
        assertTrue(city.contains(0, 0));
        assertTrue(city.contains(11, 7));
        assertFalse(city.contains(12, 0));
    }
}
