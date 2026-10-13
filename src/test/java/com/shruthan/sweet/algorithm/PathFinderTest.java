package com.shruthan.sweet.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Road;
import com.shruthan.sweet.model.RoadDirection;
import com.shruthan.sweet.model.RoadType;

class PathFinderTest {

    @Test
    void breadthFirstSearchReturnsThePathWithFewestRoads() {
        Network network = network();

        List<Intersection> path = PathFinder.findShortestPath(network.city(), network.start(), network.end());

        assertEquals(List.of("start", "end"), ids(path));
    }

    @Test
    void dijkstraReturnsTheLowestTotalRoadLengthPath() {
        Network network = network();

        List<Intersection> path = PathFinder.findWeightedPath(network.city(), network.start(), network.end());

        assertEquals(List.of("start", "middle", "end"), ids(path));
    }

    private static Network network() {
        City city = new City("city-1", "Test City", 10, 10);
        Intersection start = new Intersection("start", 1, 1);
        Intersection middle = new Intersection("middle", 3, 1);
        Intersection end = new Intersection("end", 5, 1);
        city.setIntersections(List.of(start, middle, end));
        city.addRoad(road("direct", start, end, 10));
        city.addRoad(road("first", start, middle, 2));
        city.addRoad(road("second", middle, end, 2));
        return new Network(city, start, end);
    }

    private static Road road(String id, Intersection start, Intersection end, int length) {
        return new Road(id, id, start, end, RoadType.STREET, length, true, RoadDirection.HORIZONTAL);
    }

    private static List<String> ids(List<Intersection> path) {
        return path.stream().map(Intersection::getId).toList();
    }

    private record Network(City city, Intersection start, Intersection end) {
    }
}
