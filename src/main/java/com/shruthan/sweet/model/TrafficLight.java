package com.shruthan.sweet.model;

import java.util.Objects;

public class TrafficLight {

    private String id;
    private Intersection intersection;
    private TrafficLightState state;
    private int cycleDurationSeconds;

    public TrafficLight() {
        this.state = TrafficLightState.RED;
    }

    public TrafficLight(String id, Intersection intersection) {
        this(id, intersection, TrafficLightState.RED, 30);
    }

    public TrafficLight(String id, Intersection intersection, TrafficLightState state, int cycleDurationSeconds) {
        setId(id);
        setIntersection(intersection);
        setState(state);
        setCycleDurationSeconds(cycleDurationSeconds);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Traffic light id is required");
        }
        this.id = id.trim();
    }

    public Intersection getIntersection() {
        return intersection;
    }

    public void setIntersection(Intersection intersection) {
        this.intersection = intersection;
    }

    public TrafficLightState getState() {
        return state;
    }

    public void setState(TrafficLightState state) {
        if (state == null) {
            throw new IllegalArgumentException("Traffic light state is required");
        }
        this.state = state;
    }

    public int getCycleDurationSeconds() {
        return cycleDurationSeconds;
    }

    public void setCycleDurationSeconds(int cycleDurationSeconds) {
        if (cycleDurationSeconds <= 0) {
            throw new IllegalArgumentException("Cycle duration must be positive");
        }
        this.cycleDurationSeconds = cycleDurationSeconds;
    }

    public void advance() {
        this.state = this.state.nextState();
    }

    public boolean canAdvanceTo(TrafficLightState nextState) {
        return this.state.canTransitionTo(nextState);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TrafficLight other)) {
            return false;
        }
        return Objects.equals(id, other.id)
                && Objects.equals(intersection, other.intersection)
                && state == other.state
                && cycleDurationSeconds == other.cycleDurationSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, intersection, state, cycleDurationSeconds);
    }
}
