package com.shruthan.sweet.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

public class City {

    @Id
    private String id;
    private String name;
    private int width;
    private int height;
    private List<Road> roads = new ArrayList<>();
    private List<Intersection> intersections = new ArrayList<>();
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<Building> buildings = new ArrayList<>();

    private CityGrid grid;

    public City() {
        this("city-1", "Sweet", 10, 10);
    }

    public City(String id, String name, int width, int height) {
        this(id, name, width, height, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public City(String id, String name, int width, int height, List<Road> roads,
            List<Intersection> intersections, List<Vehicle> vehicles, List<Building> buildings) {
        setId(id);
        setName(name);
        if (width <= 0) {
            throw new IllegalArgumentException("City width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("City height must be positive");
        }
        this.width = width;
        this.height = height;
        this.grid = new CityGrid(width, height);
        setRoads(roads);
        setIntersections(intersections);
        setVehicles(vehicles);
        setBuildings(buildings);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("City name is required");
        }
        this.name = name.trim();
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("City width must be positive");
        }
        this.width = width;
        this.grid = (this.width > 0 && this.height > 0) ? new CityGrid(this.width, this.height) : null;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("City height must be positive");
        }
        this.height = height;
        this.grid = (this.width > 0 && this.height > 0) ? new CityGrid(this.width, this.height) : null;
    }

    public List<Road> getRoads() {
        return roads;
    }

    public void setRoads(List<Road> roads) {
        this.roads = roads == null ? new ArrayList<>() : new ArrayList<>(roads);
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public void setIntersections(List<Intersection> intersections) {
        this.intersections = intersections == null ? new ArrayList<>() : new ArrayList<>(intersections);
        for (Intersection intersection : this.intersections) {
            if (!contains(intersection)) {
                throw new IllegalArgumentException("Intersection " + intersection.getId() + " is outside the city grid");
            }
        }
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles == null ? new ArrayList<>() : new ArrayList<>(vehicles);
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public void setBuildings(List<Building> buildings) {
        this.buildings = buildings == null ? new ArrayList<>() : new ArrayList<>(buildings);
        for (Building building : this.buildings) {
            if (!contains(building)) {
                throw new IllegalArgumentException("Building " + building.getId() + " is outside the city grid");
            }
        }
        validateBuildingPlacement();
    }

    public void addRoad(Road road) {
        if (road == null) {
            throw new IllegalArgumentException("Road cannot be null");
        }
        if (!contains(road.getStart()) || !contains(road.getEnd())) {
            throw new IllegalArgumentException("Road endpoints must be inside the city grid");
        }
        if (!intersections.contains(road.getStart())) {
            addIntersection(road.getStart());
        }
        if (!intersections.contains(road.getEnd())) {
            addIntersection(road.getEnd());
        }
        if (hasDuplicateRoad(road)) {
            throw new IllegalArgumentException("Duplicate road segment detected");
        }
        roads.add(road);
    }

    public void addIntersection(Intersection intersection) {
        if (intersection == null) {
            throw new IllegalArgumentException("Intersection cannot be null");
        }
        if (!contains(intersection)) {
            throw new IllegalArgumentException("Intersection " + intersection.getId() + " is outside the city grid");
        }
        if (!intersections.contains(intersection)) {
            intersections.add(intersection);
        }
    }

    public void addVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        vehicles.add(vehicle);
    }

    public void addBuilding(Building building) {
        if (building == null) {
            throw new IllegalArgumentException("Building cannot be null");
        }
        if (!contains(building)) {
            throw new IllegalArgumentException("Building " + building.getId() + " is outside the city grid");
        }
        for (Building existing : buildings) {
            if (existing.overlaps(building)) {
                throw new IllegalArgumentException("Building " + building.getId() + " overlaps an existing building");
            }
        }
        buildings.add(building);
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean contains(Intersection intersection) {
        return intersection != null && contains(intersection.getX(), intersection.getY());
    }

    public boolean contains(Building building) {
        if (building == null) {
            return false;
        }
        int x2 = building.getX() + building.getWidth() - 1;
        int y2 = building.getY() + building.getHeight() - 1;
        return contains(building.getX(), building.getY()) && contains(x2, y2);
    }

    public CityGrid getGrid() {
        return grid;
    }

    public void validateRoadNetwork() {
        RoadNetworkValidator.validate(this);
    }

    public boolean isValidRoadNetwork() {
        return RoadNetworkValidator.isValid(this);
    }

    private boolean hasDuplicateRoad(Road candidate) {
        for (Road road : roads) {
            if (road != null && road != candidate && road.connects(candidate.getStart(), candidate.getEnd())) {
                return true;
            }
        }
        return false;
    }

    private void validateBuildingPlacement() {
        for (int i = 0; i < buildings.size(); i++) {
            for (int j = i + 1; j < buildings.size(); j++) {
                if (buildings.get(i).overlaps(buildings.get(j))) {
                    throw new IllegalArgumentException("Building overlap detected between "
                            + buildings.get(i).getId() + " and " + buildings.get(j).getId());
                }
            }
        }
    }
}
