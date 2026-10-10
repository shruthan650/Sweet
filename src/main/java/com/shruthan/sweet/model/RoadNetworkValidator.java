package com.shruthan.sweet.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RoadNetworkValidator {

    private RoadNetworkValidator() {
    }

    public static void validate(City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }

        if (city.getIntersections() == null) {
            throw new IllegalArgumentException("City intersections list is required");
        }

        for (Intersection intersection : city.getIntersections()) {
            if (intersection == null) {
                throw new IllegalArgumentException("City contains a null intersection");
            }
            if (!city.contains(intersection)) {
                throw new IllegalArgumentException(
                        "Intersection " + intersection.getId() + " is outside the city grid");
            }
        }

        for (Road road : city.getRoads()) {
            if (road == null) {
                throw new IllegalArgumentException("City contains a null road");
            }
            if (!city.contains(road.getStart()) || !city.contains(road.getEnd())) {
                throw new IllegalArgumentException(
                        "Road " + road.getId() + " connects intersections outside the city grid");
            }
            if (road.getStart().equals(road.getEnd())) {
                throw new IllegalArgumentException(
                        "Road " + road.getId() + " cannot connect an intersection to itself");
            }
            if (hasDuplicateRoad(city.getRoads(), road)) {
                throw new IllegalArgumentException(
                        "Duplicate road segment detected for " + road.getId());
            }
        }

        for (Building building : city.getBuildings()) {
            if (building == null) {
                throw new IllegalArgumentException("City contains a null building");
            }
            if (!city.contains(building)) {
                throw new IllegalArgumentException("Building " + building.getId() + " is outside the city grid");
            }
        }

        for (int i = 0; i < city.getBuildings().size(); i++) {
            for (int j = i + 1; j < city.getBuildings().size(); j++) {
                if (city.getBuildings().get(i).overlaps(city.getBuildings().get(j))) {
                    throw new IllegalArgumentException("Building overlap detected between "
                            + city.getBuildings().get(i).getId() + " and " + city.getBuildings().get(j).getId());
                }
            }
        }

        if (city.getIntersections().isEmpty()) {
            return;
        }

        Set<Intersection> visited = new HashSet<>();
        Deque<Intersection> queue = new ArrayDeque<>();
        queue.add(city.getIntersections().getFirst());

        while (!queue.isEmpty()) {
            Intersection current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            for (Road road : city.getRoads()) {
                if (road.getStart().equals(current)) {
                    queue.addLast(road.getEnd());
                } else if (road.getEnd().equals(current)) {
                    queue.addLast(road.getStart());
                }
            }
        }

        if (visited.size() != city.getIntersections().size()) {
            throw new IllegalArgumentException("City road network contains disconnected intersections");
        }
    }

    public static boolean isValid(City city) {
        try {
            validate(city);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean hasDuplicateRoad(List<Road> roads, Road candidate) {
        int duplicates = 0;
        for (Road road : roads) {
            if (road == null || candidate == null) {
                continue;
            }
            if (road != candidate && road.connects(candidate.getStart(), candidate.getEnd())) {
                duplicates++;
            }
        }
        return duplicates > 0;
    }
}
