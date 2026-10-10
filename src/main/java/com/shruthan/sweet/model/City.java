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

    public City() {
    }

    public City(String id, String name, int width, int height) {
        this(id, name, width, height, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public City(String id, String name, int width, int height, List<Road> roads,
            List<Intersection> intersections, List<Vehicle> vehicles, List<Building> buildings) {
        setId(id);
        setName(name);
        setWidth(width);
        setHeight(height);
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
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("City height must be positive");
        }
        this.height = height;
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
    }

    public void addRoad(Road road) {
        if (road == null) {
            throw new IllegalArgumentException("Road cannot be null");
        }
        roads.add(road);
    }

    public void addIntersection(Intersection intersection) {
        if (intersection == null) {
            throw new IllegalArgumentException("Intersection cannot be null");
        }
        intersections.add(intersection);
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
        buildings.add(building);
    }

    public boolean contains(Intersection intersection) {
        return intersections.contains(intersection);
    }

    public boolean contains(Building building) {
        return buildings.contains(building);
    }
}
