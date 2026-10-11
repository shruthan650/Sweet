package com.shruthan.sweet.repository;

import java.util.List;
import java.util.Optional;

import com.shruthan.sweet.model.City;

public interface CityRepository {

    List<City> findAll();

    Optional<City> findById(String id);

    City create(City city);

    City update(String id, City city);

    void deleteById(String id);
}
