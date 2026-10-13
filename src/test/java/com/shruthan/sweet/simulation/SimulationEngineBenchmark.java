package com.shruthan.sweet.simulation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.TrafficLight;
import com.shruthan.sweet.model.TrafficLightState;
import com.shruthan.sweet.model.Vehicle;

/**
 * Repeatable local performance characterization. Run with
 * {@code mvn -Dtest=SimulationEngineBenchmark test}.
 *
 * This is deliberately not named *Test so regular unit-test runs do not use
 * wall-clock timing as a pass/fail criterion.
 */
class SimulationEngineBenchmark {

    private static final int WARMUP_TICKS = 200;
    private static final int MEASURED_TICKS = 1_000;
    private static final int SAMPLES = 3;

    @Test
    void measuresTicksAcrossCitySizesAndRouteLengths() {
        measure("small (100 intersections, 50 vehicles)", () -> city(100, 50, 0));
        measure("medium (500 intersections, 250 vehicles)", () -> city(500, 250, 0));
        measure("large (1,000 intersections, 500 vehicles)", () -> city(1_000, 500, 0));
        measure("long routes (1,000 intersections, 20 vehicles, 2,000 route nodes each)",
                () -> city(1_000, 20, 2_000));
    }

    private static void measure(String name, CityFactory cityFactory) {
        long[] nanosPerTick = new long[SAMPLES];
        for (int sample = 0; sample < SAMPLES; sample++) {
            SimulationEngine engine = new SimulationEngine(cityFactory.create());
            engine.start();
            runTicks(engine, WARMUP_TICKS);
            long startedAt = System.nanoTime();
            runTicks(engine, MEASURED_TICKS);
            nanosPerTick[sample] = (System.nanoTime() - startedAt) / MEASURED_TICKS;
        }
        Arrays.sort(nanosPerTick);
        long medianNanos = nanosPerTick[SAMPLES / 2];
        System.out.printf("%s: median %.3f ms/tick (%d measured ticks per sample)%n",
                name, medianNanos / 1_000_000.0, MEASURED_TICKS);
        assertTrue(medianNanos > 0, "Benchmark should record a positive elapsed time");
    }

    private static void runTicks(SimulationEngine engine, int count) {
        for (int tick = 0; tick < count; tick++) {
            engine.tick();
        }
    }

    private static City city(int intersectionCount, int vehicleCount, int routeLength) {
        int width = 1_000;
        int height = Math.max(1, (intersectionCount + width - 1) / width);
        City city = new City("benchmark-city", "Benchmark", width, height);
        List<Intersection> intersections = new ArrayList<>(intersectionCount);
        for (int index = 0; index < intersectionCount; index++) {
            Intersection intersection = new Intersection("i-%d".formatted(index), index % width, index / width);
            intersection.setTrafficLight(new TrafficLight("light-%d".formatted(index), intersection,
                    TrafficLightState.RED, 30));
            intersections.add(intersection);
        }
        city.setIntersections(intersections);

        for (int index = 0; index < vehicleCount; index++) {
            Intersection position = intersections.get(index % intersections.size());
            Vehicle vehicle = new Vehicle("v-%d".formatted(index), "Benchmark vehicle",
                    position.getX(), position.getY(), 30);
            vehicle.setCurrentIntersection(position);
            if (routeLength > 0) {
                List<Intersection> route = new ArrayList<>(routeLength);
                for (int routeIndex = 0; routeIndex < routeLength; routeIndex++) {
                    route.add(intersections.get(routeIndex % intersections.size()));
                }
                vehicle.setRoute(route);
                vehicle.setDestination(route.getLast());
            }
            city.addVehicle(vehicle);
        }
        return city;
    }

    @FunctionalInterface
    private interface CityFactory {
        City create();
    }
}
