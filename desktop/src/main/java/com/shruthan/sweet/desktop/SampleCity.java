package com.shruthan.sweet.desktop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** A busy starter map for demonstrating routes, vehicles, and traffic lights. */
final class SampleCity {

    static final String ID = "sweet-traffic-demo-v3";
    static final String NAME = "Sweet Traffic Demo";
    static final String LEGACY_ID = "sweet-demo-v1";
    static final String PREVIOUS_DEMO_ID = "sweet-traffic-demo-v2";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int[] X_COORDINATES = { 2, 8, 14, 20 };
    private static final int[] Y_COORDINATES = { 2, 7, 12, 16 };
    private static final String[] COLUMN_NAMES = { "West End", "Old Town", "Downtown", "East Gate" };
    private static final String[] ROW_NAMES = { "North Heights", "Market District", "Civic Center", "South Park" };
    private static final String[] HORIZONTAL_ROADS = {
            "North Boulevard", "Market Avenue", "Civic Avenue", "South Street"
    };
    private static final String[] VERTICAL_ROADS = {
            "Harbor Road", "Pine Street", "Central Avenue", "Airport Road"
    };

    private SampleCity() {
    }

    static String json() {
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(buildCity());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not build the sample city", exception);
        }
    }

    private static ObjectNode buildCity() {
        List<IntersectionSpec> intersections = buildIntersections();
        Map<String, IntersectionSpec> intersectionsById = new HashMap<>();
        Map<String, List<RoadSpec>> incomingRoads = new HashMap<>();
        Map<String, List<RoadSpec>> outgoingRoads = new HashMap<>();
        for (IntersectionSpec intersection : intersections) {
            intersectionsById.put(intersection.id(), intersection);
            incomingRoads.put(intersection.id(), new ArrayList<>());
            outgoingRoads.put(intersection.id(), new ArrayList<>());
        }

        List<RoadSpec> roads = buildRoads(intersectionsById);
        for (RoadSpec road : roads) {
            incomingRoads.get(road.startId()).add(road);
            outgoingRoads.get(road.startId()).add(road);
            incomingRoads.get(road.endId()).add(road);
            outgoingRoads.get(road.endId()).add(road);
        }

        ObjectNode city = JSON.createObjectNode();
        city.put("id", ID);
        city.put("name", NAME);
        city.put("width", 24);
        city.put("height", 18);

        ArrayNode roadArray = city.putArray("roads");
        roads.stream().map(road -> roadJson(road, intersectionsById)).forEach(roadArray::add);

        ArrayNode intersectionArray = city.putArray("intersections");
        for (IntersectionSpec intersection : intersections) {
            ObjectNode node = intersectionJson(intersection);
            ArrayNode incoming = node.putArray("incomingRoads");
            incomingRoads.get(intersection.id()).stream()
                    .map(road -> roadJson(road, intersectionsById)).forEach(incoming::add);
            ArrayNode outgoing = node.putArray("outgoingRoads");
            outgoingRoads.get(intersection.id()).stream()
                    .map(road -> roadJson(road, intersectionsById)).forEach(outgoing::add);
            addTrafficLight(node, intersection);
            intersectionArray.add(node);
        }

        ArrayNode vehicleArray = city.putArray("vehicles");
        buildVehicles().stream()
                .map(vehicle -> vehicleJson(vehicle, intersectionsById))
                .forEach(vehicleArray::add);

        ArrayNode buildingArray = city.putArray("buildings");
        buildBuildings().forEach(building -> buildingArray.add(buildingJson(building)));
        return city;
    }

    private static List<IntersectionSpec> buildIntersections() {
        List<IntersectionSpec> intersections = new ArrayList<>();
        for (int row = 0; row < Y_COORDINATES.length; row++) {
            for (int column = 0; column < X_COORDINATES.length; column++) {
                intersections.add(intersection(column, row));
            }
        }
        return intersections;
    }

    private static List<RoadSpec> buildRoads(Map<String, IntersectionSpec> intersections) {
        List<RoadSpec> roads = new ArrayList<>();
        for (int row = 0; row < Y_COORDINATES.length; row++) {
            for (int column = 0; column < X_COORDINATES.length - 1; column++) {
                roads.add(road("road-h-" + row + "-" + column, HORIZONTAL_ROADS[row],
                        column, row, column + 1, row, "HORIZONTAL",
                        row == 0 || row == 3 ? "STREET" : "AVENUE", intersections));
            }
        }
        for (int column = 0; column < X_COORDINATES.length; column++) {
            for (int row = 0; row < Y_COORDINATES.length - 1; row++) {
                roads.add(road("road-v-" + column + "-" + row, VERTICAL_ROADS[column],
                        column, row, column, row + 1, "VERTICAL",
                        column == 2 ? "AVENUE" : "STREET", intersections));
            }
        }
        return roads;
    }

    private static RoadSpec road(String id, String name, int startColumn, int startRow,
            int endColumn, int endRow, String direction, String type,
            Map<String, IntersectionSpec> intersections) {
        String startId = intersectionId(startColumn, startRow);
        String endId = intersectionId(endColumn, endRow);
        IntersectionSpec start = intersections.get(startId);
        IntersectionSpec end = intersections.get(endId);
        int length = Math.max(Math.abs(start.x() - end.x()), Math.abs(start.y() - end.y()));
        return new RoadSpec(id, name, startId, endId, direction, type, length);
    }

    private static ObjectNode roadJson(RoadSpec road, Map<String, IntersectionSpec> intersections) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", road.id());
        node.put("name", road.name());
        node.set("start", intersectionReference(intersections.get(road.startId())));
        node.set("end", intersectionReference(intersections.get(road.endId())));
        node.put("type", road.type());
        node.put("direction", road.direction());
        node.put("bidirectional", true);
        node.put("length", road.length());
        return node;
    }

    private static ObjectNode intersectionJson(IntersectionSpec intersection) {
        ObjectNode node = intersectionReference(intersection);
        return node;
    }

    private static ObjectNode intersectionReference(IntersectionSpec intersection) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", intersection.id());
        node.put("name", intersection.name());
        node.put("x", intersection.x());
        node.put("y", intersection.y());
        return node;
    }

    private static void addTrafficLight(ObjectNode intersection, IntersectionSpec spec) {
        if (!hasTrafficLight(spec)) {
            intersection.putNull("trafficLight");
            return;
        }
        ObjectNode light = intersection.putObject("trafficLight");
        light.put("id", "signal-" + spec.id());
        light.put("state", spec.column() == 1 && spec.row() == 1 ? "GREEN" : "RED");
        light.put("cycleDurationSeconds", 12);
    }

    private static boolean hasTrafficLight(IntersectionSpec intersection) {
        return (intersection.column() == 1 && intersection.row() == 1)
                || (intersection.column() == 2 && intersection.row() == 1)
                || (intersection.column() == 2 && intersection.row() == 2)
                || (intersection.column() == 1 && intersection.row() == 2);
    }

    private static List<VehicleSpec> buildVehicles() {
        return List.of(
                vehicle("car-01", "North Commuter", 0, 0, 35, 1, 0, 2, 0, 3, 0),
                vehicle("car-02", "Market Taxi", 3, 1, 28, 2, 1, 1, 1, 0, 1),
                vehicle("car-03", "Civic Shuttle", 0, 2, 32, 1, 2, 2, 2, 3, 2),
                vehicle("car-04", "Airport Van", 3, 3, 45, 3, 2, 3, 1, 3, 0),
                vehicle("car-05", "Pine Delivery", 1, 3, 24, 1, 2, 1, 1, 1, 0),
                vehicle("car-06", "Central Bus", 2, 0, 30, 2, 1, 2, 2, 2, 3),
                vehicle("car-07", "Harbor Courier", 0, 1, 38, 0, 2, 0, 3),
                vehicle("car-08", "Downtown Rideshare", 3, 2, 26, 2, 2, 1, 2, 1, 1),
                vehicle("car-09", "Southbound Bus", 2, 3, 34, 2, 2, 2, 1, 1, 1),
                vehicle("car-10", "Old Town Taxi", 1, 0, 29, 1, 1, 2, 1, 3, 1),
                vehicle("car-11", "Park Service", 0, 3, 22, 0, 2, 0, 1, 0, 0),
                vehicle("car-12", "Eastbound Delivery", 3, 0, 40, 2, 0, 2, 1, 1, 1),
                vehicle("car-13", "Morning Commuter", 1, 0, 31, 1, 1, 1, 2, 1, 3),
                vehicle("car-14", "Market Delivery", 2, 0, 26, 2, 1, 1, 1, 0, 1),
                vehicle("car-15", "School Bus", 0, 1, 24, 1, 1, 2, 1, 3, 1),
                vehicle("car-16", "Civic Taxi", 3, 1, 36, 2, 1, 2, 2, 1, 3),
                vehicle("car-17", "South Courier", 0, 2, 29, 0, 3, 1, 3, 2, 3),
                vehicle("car-18", "Airport Shuttle", 1, 2, 42, 2, 2, 3, 2, 3, 1),
                vehicle("car-19", "Evening Rideshare", 2, 2, 33, 2, 1, 1, 1, 0, 1),
                vehicle("car-20", "Harbor Bus", 3, 2, 27, 3, 1, 2, 1, 1, 0));
    }

    private static VehicleSpec vehicle(String id, String name, int startColumn, int startRow, int speedKph,
            int... routeCoordinates) {
        List<Position> route = new ArrayList<>();
        for (int index = 0; index < routeCoordinates.length; index += 2) {
            route.add(new Position(routeCoordinates[index], routeCoordinates[index + 1]));
        }
        return new VehicleSpec(id, name, startColumn, startRow, speedKph, route);
    }

    private static ObjectNode vehicleJson(VehicleSpec vehicle, Map<String, IntersectionSpec> intersections) {
        IntersectionSpec start = intersection(vehicle.startColumn(), vehicle.startRow());
        IntersectionSpec destination = vehicle.route().getLast().asIntersection();
        ObjectNode node = JSON.createObjectNode();
        node.put("id", vehicle.id());
        node.put("name", vehicle.name());
        node.put("x", start.x());
        node.put("y", start.y());
        node.put("speedKph", vehicle.speedKph());
        node.put("status", "MOVING");
        node.set("currentIntersection", intersectionReference(intersections.get(start.id())));
        node.set("destination", intersectionReference(intersections.get(destination.id())));
        ArrayNode route = node.putArray("route");
        vehicle.route().stream()
                .map(Position::asIntersection)
                .map(position -> intersections.get(position.id()))
                .map(SampleCity::intersectionReference)
                .forEach(route::add);
        return node;
    }

    private static List<BuildingSpec> buildBuildings() {
        return List.of(
                building("homes-north", "North Heights Homes", 3, 0, 3, 2, "RESIDENTIAL"),
                building("library-north", "North Library", 9, 0, 3, 2, "PUBLIC"),
                building("shops-north", "East Market Shops", 15, 0, 3, 2, "COMMERCIAL"),
                building("market-west", "West Market", 3, 3, 3, 3, "COMMERCIAL"),
                building("offices-market", "Market Offices", 9, 3, 3, 3, "COMMERCIAL"),
                building("clinic-market", "Market Clinic", 15, 3, 3, 3, "PUBLIC"),
                building("homes-civic", "Civic Apartments", 3, 8, 3, 3, "RESIDENTIAL"),
                building("school-civic", "Civic School", 9, 8, 3, 3, "PUBLIC"),
                building("factory-civic", "Civic Works", 15, 8, 3, 3, "INDUSTRIAL"),
                building("homes-south", "South Park Homes", 3, 13, 3, 2, "RESIDENTIAL"),
                building("station-south", "South Station", 9, 13, 3, 2, "PUBLIC"),
                building("depot-south", "Airport Depot", 15, 13, 3, 2, "INDUSTRIAL"));
    }

    private static BuildingSpec building(String id, String name, int x, int y,
            int width, int height, String type) {
        return new BuildingSpec(id, name, x, y, width, height, type);
    }

    private static ObjectNode buildingJson(BuildingSpec building) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", building.id());
        node.put("name", building.name());
        node.put("x", building.x());
        node.put("y", building.y());
        node.put("width", building.width());
        node.put("height", building.height());
        node.put("type", building.type());
        return node;
    }

    private static IntersectionSpec intersection(int column, int row) {
        String id = intersectionId(column, row);
        return new IntersectionSpec(id,
                ROW_NAMES[row] + " / " + COLUMN_NAMES[column],
                X_COORDINATES[column], Y_COORDINATES[row], column, row);
    }

    private static String intersectionId(int column, int row) {
        return "junction-" + column + "-" + row;
    }

    private record IntersectionSpec(String id, String name, int x, int y, int column, int row) {
    }

    private record RoadSpec(String id, String name, String startId, String endId,
            String direction, String type, int length) {
    }

    private record Position(int column, int row) {
        private IntersectionSpec asIntersection() {
            return SampleCity.intersection(column, row);
        }
    }

    private record VehicleSpec(String id, String name, int startColumn, int startRow,
            int speedKph, List<Position> route) {
    }

    private record BuildingSpec(String id, String name, int x, int y,
            int width, int height, String type) {
    }
}
