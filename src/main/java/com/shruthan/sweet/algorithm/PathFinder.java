package com.shruthan.sweet.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Road;

public final class PathFinder {

    private PathFinder() {
    }

    public static List<Intersection> findPath(City city, Intersection start, Intersection end) {
        return bfs(city, start, end);
    }

    public static List<Intersection> findShortestPath(City city, Intersection start, Intersection end) {
        return bfs(city, start, end);
    }

    public static List<Intersection> findWeightedPath(City city, Intersection start, Intersection end) {
        return dijkstra(city, start, end);
    }

    private static List<Intersection> bfs(City city, Intersection start, Intersection end) {
        validate(city, start, end);
        start = canonicalIntersection(city, start);
        end = canonicalIntersection(city, end);
        Map<Intersection, Intersection> previous = new HashMap<>();
        Deque<Intersection> queue = new ArrayDeque<>();
        Set<Intersection> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Intersection current = queue.removeFirst();
            if (sameIntersection(current, end)) {
                return reconstructPath(previous, end);
            }
            for (Intersection neighbor : neighbors(city, current)) {
                if (visited.add(neighbor)) {
                    previous.put(neighbor, current);
                    queue.addLast(neighbor);
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<Intersection> dijkstra(City city, Intersection start, Intersection end) {
        validate(city, start, end);
        start = canonicalIntersection(city, start);
        end = canonicalIntersection(city, end);
        Map<Intersection, Integer> distance = new HashMap<>();
        Map<Intersection, Intersection> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>((a, b) -> Integer.compare(a.distance, b.distance));

        for (Intersection intersection : city.getIntersections()) {
            distance.put(intersection, Integer.MAX_VALUE);
        }
        distance.put(start, 0);
        queue.add(new Node(start, 0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (sameIntersection(current.intersection, end)) {
                return reconstructPath(previous, end);
            }
            for (Intersection neighbor : neighbors(city, current.intersection)) {
                int nextDistance = distance.get(current.intersection) + getRoadWeight(city, current.intersection, neighbor);
                if (nextDistance < distance.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distance.put(neighbor, nextDistance);
                    previous.put(neighbor, current.intersection);
                    queue.add(new Node(neighbor, nextDistance));
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<Intersection> neighbors(City city, Intersection intersection) {
        List<Intersection> neighbors = new ArrayList<>();
        for (Road road : city.getRoads()) {
            if (sameIntersection(road.getStart(), intersection)) {
                addCanonicalNeighbor(city, neighbors, road.getEnd());
            } else if (road.isBidirectional() && sameIntersection(road.getEnd(), intersection)) {
                addCanonicalNeighbor(city, neighbors, road.getStart());
            }
        }
        return neighbors;
    }

    private static void addCanonicalNeighbor(City city, List<Intersection> neighbors, Intersection candidate) {
        city.getIntersections().stream()
                .filter(intersection -> sameIntersection(intersection, candidate))
                .findFirst()
                .ifPresent(neighbors::add);
    }

    private static int getRoadWeight(City city, Intersection from, Intersection to) {
        for (Road road : city.getRoads()) {
            if ((sameIntersection(road.getStart(), from) && sameIntersection(road.getEnd(), to))
                    || (road.isBidirectional()
                            && sameIntersection(road.getEnd(), from) && sameIntersection(road.getStart(), to))) {
                return road.getLength();
            }
        }
        return 1;
    }

    private static List<Intersection> reconstructPath(Map<Intersection, Intersection> previous, Intersection end) {
        List<Intersection> path = new ArrayList<>();
        Intersection current = end;
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static void validate(City city, Intersection start, Intersection end) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end intersections are required");
        }
        if (city.getIntersections().isEmpty()) {
            throw new IllegalArgumentException("City does not have any intersections");
        }
    }

    private static boolean sameIntersection(Intersection first, Intersection second) {
        return first == second || (first != null && second != null
                && Objects.equals(first.getId(), second.getId()));
    }

    private static Intersection canonicalIntersection(City city, Intersection intersection) {
        return city.getIntersections().stream()
                .filter(candidate -> sameIntersection(candidate, intersection))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Intersection is not part of city: " + intersection.getId()));
    }

    private record Node(Intersection intersection, int distance) {
    }
}
