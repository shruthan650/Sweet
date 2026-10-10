package com.shruthan.sweet.simulation;

import java.time.Instant;

public class SimulationEvent {

    private String id;
    private long tick;
    private String type;
    private String message;
    private Instant timestamp;

    public SimulationEvent() {
    }

    public SimulationEvent(String id, long tick, String type, String message) {
        setId(id);
        setTick(tick);
        setType(type);
        setMessage(message);
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Simulation event id is required");
        }
        this.id = id.trim();
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("Simulation tick cannot be negative");
        }
        this.tick = tick;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Simulation event type is required");
        }
        this.type = type.trim();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Simulation event message is required");
        }
        this.message = message.trim();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp == null ? Instant.now() : timestamp;
    }
}
