package com.shruthan.sweet.dto;

import java.util.List;

import com.shruthan.sweet.model.Building;
import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Road;
import com.shruthan.sweet.model.Vehicle;

public record CityDto(
        String id,
        String name,
        int width,
        int height,
        List<Road> roads,
        List<Intersection> intersections,
        List<Vehicle> vehicles,
        List<Building> buildings) {

    public static CityDto from(City city) {
        if (city == null) {
            return null;
        }
        return new CityDto(
                city.getId(),
                city.getName(),
                city.getWidth(),
                city.getHeight(),
                city.getRoads(),
                city.getIntersections(),
                city.getVehicles(),
                city.getBuildings());
    }

    public City toCity() {
        return new City(id, name, width, height, roads, intersections, vehicles, buildings);
    }
}
