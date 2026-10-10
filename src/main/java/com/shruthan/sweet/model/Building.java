package com.shruthan.sweet.model;

public class Building {

    private String id;
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    private BuildingType type;

    public Building() {
    }

    public Building(String id, String name, int x, int y, int width, int height, BuildingType type) {
        setId(id);
        setName(name);
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        setType(type);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Building id is required");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Building name is required");
        }
        this.name = name.trim();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("Building x coordinate must be non-negative");
        }
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if (y < 0) {
            throw new IllegalArgumentException("Building y coordinate must be non-negative");
        }
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Building width must be positive");
        }
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Building height must be positive");
        }
        this.height = height;
    }

    public BuildingType getType() {
        return type;
    }

    public void setType(BuildingType type) {
        if (type == null) {
            throw new IllegalArgumentException("Building type is required");
        }
        this.type = type;
    }

    public boolean overlaps(Building other) {
        if (other == null) {
            return false;
        }
        return x < other.x + other.width
                && x + width > other.x
                && y < other.y + other.height
                && y + height > other.y;
    }
}
