package com.shruthan.sweet.model;

public class CityGrid {

    private final int width;
    private final int height;

    public CityGrid(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("City width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("City height must be positive");
        }
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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
        return contains(building.getX(), building.getY())
                && contains(building.getX() + building.getWidth() - 1, building.getY() + building.getHeight() - 1);
    }
}
