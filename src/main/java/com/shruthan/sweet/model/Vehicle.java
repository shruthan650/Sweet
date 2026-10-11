package com.shruthan.sweet.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehicle {

    private String id;
    private String name;
    private int x;
    private int y;
    private int speedKph;
    private VehicleStatus status;
    private Intersection currentIntersection;
    private List<Intersection> route = new ArrayList<>();
    private Intersection destination;

    public Vehicle() {
    }

    public Vehicle(String id, String name, int x, int y, int speedKph) {
        setId(id);
        setName(name);
        setX(x);
        setY(y);
        setSpeedKph(speedKph);
        this.status = VehicleStatus.IDLE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Vehicle name is required");
        }
        this.name = name.trim();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSpeedKph() {
        return speedKph;
    }

    public void setSpeedKph(int speedKph) {
        if (speedKph <= 0) {
            throw new IllegalArgumentException("Vehicle speed must be positive");
        }
        this.speedKph = speedKph;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Vehicle status is required");
        }
        this.status = status;
    }

    public Intersection getCurrentIntersection() {
        return currentIntersection;
    }

    public void setCurrentIntersection(Intersection currentIntersection) {
        this.currentIntersection = currentIntersection;
    }

    public List<Intersection> getRoute() {
        return route;
    }

    public void setRoute(List<Intersection> route) {
        this.route = route == null ? new ArrayList<>() : new ArrayList<>(route);
    }

    public Intersection getDestination() {
        return destination;
    }

    public void setDestination(Intersection destination) {
        this.destination = destination;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
        this.status = VehicleStatus.MOVING;
    }

    public void advanceRoute() {
        if (route == null || route.isEmpty()) {
            if (destination != null && currentIntersection != null && destination.equals(currentIntersection)) {
                status = VehicleStatus.ARRIVED;
            } else if (status != VehicleStatus.ARRIVED) {
                status = VehicleStatus.IDLE;
            }
            return;
        }

        Intersection next = route.remove(0);
        currentIntersection = next;
        x = next.getX();
        y = next.getY();
        status = destination != null && destination.equals(next) ? VehicleStatus.ARRIVED : VehicleStatus.MOVING;
    }

    public boolean hasReachedDestination() {
        return destination != null && currentIntersection != null && destination.equals(currentIntersection);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Vehicle other)) {
            return false;
        }
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && x == other.x
                && y == other.y
                && speedKph == other.speedKph
                && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, x, y, speedKph, status);
    }
}
