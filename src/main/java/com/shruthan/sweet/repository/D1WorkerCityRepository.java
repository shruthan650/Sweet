package com.shruthan.sweet.repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.shruthan.sweet.model.City;

@Repository
public class D1WorkerCityRepository implements CityRepository {

    private static final ParameterizedTypeReference<List<City>> CITY_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final boolean configured;

    public D1WorkerCityRepository(
            RestClient.Builder restClientBuilder,
            @Value("${sweet.worker.base-url:http://localhost:8787}") String workerBaseUrl,
            @Value("${sweet.worker.shared-secret:}") String sharedSecret) {
        URI workerUri;
        try {
            workerUri = URI.create(workerBaseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Worker URL must be a valid HTTPS URL", exception);
        }
        boolean localHttp = "http".equals(workerUri.getScheme())
                && ("localhost".equals(workerUri.getHost()) || "127.0.0.1".equals(workerUri.getHost())
                        || "[::1]".equals(workerUri.getHost()));
        boolean secureHttps = "https".equals(workerUri.getScheme()) && workerUri.getHost() != null;
        if (!(localHttp || secureHttps) || workerUri.getUserInfo() != null
                || workerUri.getQuery() != null || workerUri.getFragment() != null
                || (workerUri.getPath() != null && !workerUri.getPath().isEmpty()
                        && !"/".equals(workerUri.getPath()))) {
            throw new IllegalArgumentException("Worker URL must use HTTPS outside localhost");
        }
        this.configured = sharedSecret != null && sharedSecret.length() >= 32;
        RestClient.Builder builder = restClientBuilder.baseUrl(workerUri.toString());
        if (configured) {
            builder.defaultHeader("Authorization", "Bearer " + sharedSecret);
        }
        this.restClient = builder.build();
    }

    @Override
    public List<City> findAll() {
        requireConfigured();
        List<City> cities = restClient.get()
                .uri("/v1/cities")
                .retrieve()
                .body(CITY_LIST_TYPE);
        if (cities == null) {
            throw new IllegalStateException("Cloudflare Worker returned an empty city list response");
        }
        return cities;
    }

    @Override
    public Optional<City> findById(String id) {
        requireConfigured();
        try {
            City city = restClient.get()
                    .uri(builder -> builder.pathSegment("v1", "cities", id).build())
                    .retrieve()
                    .body(City.class);
            if (city == null) {
                throw new IllegalStateException("Cloudflare Worker returned an empty city response");
            }
            return Optional.of(city);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    @Override
    public City create(City city) {
        requireConfigured();
        City saved = restClient.post()
                .uri("/v1/cities")
                .body(CityPayload.from(city))
                .retrieve()
                .body(City.class);
        if (saved == null) {
            throw new IllegalStateException("Cloudflare Worker returned an empty city response");
        }
        return saved;
    }

    @Override
    public City update(String id, City city) {
        requireConfigured();
        City saved = restClient.put()
                .uri(builder -> builder.pathSegment("v1", "cities", id).build())
                .body(CityPayload.from(city))
                .retrieve()
                .body(City.class);
        if (saved == null) {
            throw new IllegalStateException("Cloudflare Worker returned an empty city response");
        }
        return saved;
    }

    @Override
    public void deleteById(String id) {
        requireConfigured();
        restClient.delete()
                .uri(builder -> builder.pathSegment("v1", "cities", id).build())
                .retrieve()
                .toBodilessEntity();
    }

    private void requireConfigured() {
        if (!configured) {
            throw new IllegalStateException(
                    "Cloudflare Worker access is not configured; set SWEET_WORKER_SHARED_SECRET to a secret of at least 32 characters");
        }
    }

    private record CityPayload(
            String id,
            String name,
            int width,
            int height,
            List<RoadPayload> roads,
            List<IntersectionPayload> intersections,
            List<VehiclePayload> vehicles,
            List<BuildingPayload> buildings) {

        private static CityPayload from(City city) {
            rejectNullItems(city.getRoads(), "roads");
            rejectNullItems(city.getIntersections(), "intersections");
            rejectNullItems(city.getVehicles(), "vehicles");
            rejectNullItems(city.getBuildings(), "buildings");
            List<RoadPayload> roads = city.getRoads().stream()
                    .map(road -> new RoadPayload(road.getId(), road.getName(), road.getStart().getId(),
                            road.getEnd().getId(), enumName(road.getType()), enumName(road.getDirection()),
                            road.isBidirectional(), road.getLength()))
                    .toList();
            List<IntersectionPayload> intersections = city.getIntersections().stream()
                    .map(intersection -> {
                        rejectNullItems(intersection.getIncomingRoads(), "incoming roads");
                        rejectNullItems(intersection.getOutgoingRoads(), "outgoing roads");
                        var light = intersection.getTrafficLight();
                        TrafficLightPayload lightPayload = light == null ? null
                                : new TrafficLightPayload(light.getId(), enumName(light.getState()),
                                        light.getCycleDurationSeconds());
                        return new IntersectionPayload(intersection.getId(), intersection.getName(),
                                intersection.getX(), intersection.getY(), lightPayload,
                                intersection.getIncomingRoads().stream().map(road -> road.getId()).toList(),
                                intersection.getOutgoingRoads().stream().map(road -> road.getId()).toList());
                    })
                    .toList();
            List<VehiclePayload> vehicles = city.getVehicles().stream()
                    .map(vehicle -> new VehiclePayload(vehicle.getId(), vehicle.getName(), vehicle.getX(),
                            vehicle.getY(), vehicle.getSpeedKph(), enumName(vehicle.getStatus()),
                            idOf(vehicle.getCurrentIntersection()),
                            vehicle.getRoute().stream().map(intersection -> intersection.getId()).toList(),
                            idOf(vehicle.getDestination())))
                    .toList();
            List<BuildingPayload> buildings = city.getBuildings().stream()
                    .map(building -> new BuildingPayload(building.getId(), building.getName(), building.getX(),
                            building.getY(), building.getWidth(), building.getHeight(), enumName(building.getType())))
                    .toList();
            return new CityPayload(city.getId(), city.getName(), city.getWidth(), city.getHeight(),
                    roads, intersections, vehicles, buildings);
        }

        private static String idOf(com.shruthan.sweet.model.Intersection intersection) {
            return intersection == null ? null : intersection.getId();
        }

        private static String enumName(Enum<?> value) {
            return value == null ? null : value.name();
        }

        private static void rejectNullItems(List<?> items, String label) {
            if (items.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("City " + label + " cannot contain null entries");
            }
        }
    }

    private record RoadPayload(String id, String name, String start, String end,
            String type, String direction, boolean bidirectional, int length) {
    }

    private record TrafficLightPayload(String id, String state, int cycleDurationSeconds) {
    }

    private record IntersectionPayload(String id, String name, int x, int y,
            TrafficLightPayload trafficLight, List<String> incomingRoads, List<String> outgoingRoads) {
    }

    private record VehiclePayload(String id, String name, int x, int y, int speedKph,
            String status, String currentIntersection, List<String> route, String destination) {
    }

    private record BuildingPayload(String id, String name, int x, int y, int width, int height, String type) {
    }
}
