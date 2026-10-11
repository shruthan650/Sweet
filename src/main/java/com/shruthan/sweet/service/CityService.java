package com.shruthan.sweet.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.repository.CityRepository;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<City> findAll() {
        return cityRepository.findAll();
    }

    public Optional<City> findById(String id) {
        return cityRepository.findById(id);
    }

    public City create(City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        return cityRepository.save(city);
    }

    public City update(String id, City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        if (!cityRepository.existsById(id)) {
            throw new IllegalArgumentException("City not found: " + id);
        }
        city.setId(id);
        return cityRepository.save(city);
    }

    public void delete(String id) {
        cityRepository.deleteById(id);
    }
}
