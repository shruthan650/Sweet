package com.shruthan.sweet.model;

import java.util.ArrayList;
import java.util.List;

public class Intersection {

    private String id;
    private String name;
    private int x;
    private int y;
    private List<Road> incomingRoads = new ArrayList<>();
    private List<Road> outgoingRoads = new ArrayList<>();
    private TrafficLight trafficLight;

    public Intersection() {
    }

    public Intersection(String id, int x, int y) {
        this(id, "Intersection-" + id, x, y);
    }

    public Intersection(String id, String name, int x, int y) {
        setId(id);
        setName(name);
        setX(x);
        setY(y);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Intersection id is required");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Intersection name is required");
        }
        this.name = name.trim();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("Intersection x coordinate must be non-negative");
        }
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if (y < 0) {
            throw new IllegalArgumentException("Intersection y coordinate must be non-negative");
        }
        this.y = y;
    }

    public List<Road> getIncomingRoads() {
        return incomingRoads;
    }

    public void setIncomingRoads(List<Road> incomingRoads) {
        this.incomingRoads = incomingRoads == null ? new ArrayList<>() : new ArrayList<>(incomingRoads);
    }

    public List<Road> getOutgoingRoads() {
        return outgoingRoads;
    }

    public void setOutgoingRoads(List<Road> outgoingRoads) {
        this.outgoingRoads = outgoingRoads == null ? new ArrayList<>() : new ArrayList<>(outgoingRoads);
    }

    public void addIncomingRoad(Road road) {
        if (road == null) {
            throw new IllegalArgumentException("Incoming road cannot be null");
        }
        incomingRoads.add(road);
    }

    public void addOutgoingRoad(Road road) {
        if (road == null) {
            throw new IllegalArgumentException("Outgoing road cannot be null");
        }
        outgoingRoads.add(road);
    }

    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }

    public boolean matches(int x, int y) {
        return this.x == x && this.y == y;
    }
}
