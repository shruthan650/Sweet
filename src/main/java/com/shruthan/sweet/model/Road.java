package com.shruthan.sweet.model;

import java.util.Objects;

public class Road {

    private String id;
    private String name;
    private Intersection start;
    private Intersection end;
    private RoadType type;
    private RoadDirection direction;
    private boolean bidirectional;
    private int length;

    public Road() {
    }

    public Road(String id, String name, Intersection start, Intersection end, RoadType type) {
        this(id, name, start, end, type, calculateLength(start, end), true, determineDirection(start, end));
    }

    public Road(String id, String name, Intersection start, Intersection end, RoadType type,
            int length, boolean bidirectional, RoadDirection direction) {
        setId(id);
        setName(name);
        setStart(start);
        setEnd(end);
        setType(type);
        setLength(length);
        setBidirectional(bidirectional);
        setDirection(direction);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Road id is required");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Road name is required");
        }
        this.name = name.trim();
    }

    public Intersection getStart() {
        return start;
    }

    public void setStart(Intersection start) {
        if (start == null) {
            throw new IllegalArgumentException("Road start intersection is required");
        }
        this.start = start;
    }

    public Intersection getEnd() {
        return end;
    }

    public void setEnd(Intersection end) {
        if (end == null) {
            throw new IllegalArgumentException("Road end intersection is required");
        }
        this.end = end;
    }

    public RoadType getType() {
        return type;
    }

    public void setType(RoadType type) {
        if (type == null) {
            throw new IllegalArgumentException("Road type is required");
        }
        this.type = type;
    }

    public RoadDirection getDirection() {
        return direction;
    }

    public void setDirection(RoadDirection direction) {
        this.direction = direction == null ? determineDirection(start, end) : direction;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Road length must be positive");
        }
        this.length = length;
    }

    public static int calculateLength(Intersection start, Intersection end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Road endpoints are required");
        }
        return Math.max(Math.abs(start.getX() - end.getX()), Math.abs(start.getY() - end.getY()));
    }

    public boolean connects(Intersection one, Intersection two) {
        return (start.equals(one) && end.equals(two)) || (start.equals(two) && end.equals(one));
    }

    public boolean isSameSegment(Intersection one, Intersection two) {
        return connects(one, two);
    }

    public static RoadDirection determineDirection(Intersection start, Intersection end) {
        if (start == null || end == null) {
            return RoadDirection.UNKNOWN;
        }
        if (start.getX() == end.getX() && start.getY() != end.getY()) {
            return RoadDirection.VERTICAL;
        }
        if (start.getY() == end.getY() && start.getX() != end.getX()) {
            return RoadDirection.HORIZONTAL;
        }
        if (start.getX() != end.getX() && start.getY() != end.getY()) {
            return RoadDirection.DIAGONAL;
        }
        return RoadDirection.UNKNOWN;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Road other)) {
            return false;
        }
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(start, other.start)
                && Objects.equals(end, other.end)
                && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, start, end, type);
    }
}
