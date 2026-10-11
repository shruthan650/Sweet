package com.shruthan.sweet.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        if (city.getId() == null || city.getId().isBlank()) {
            city.setId(UUID.randomUUID().toString());
        }
        return cityRepository.create(city);
    }

    public City update(String id, City city) {
        if (city == null) {
            throw new IllegalArgumentException("City is required");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("City id is required");
        }
        if (cityRepository.findById(id).isEmpty()) {
            throw new CityNotFoundException(id);
        }
        city.setId(id);
        return cityRepository.update(id, city);
    }

    public void delete(String id) {
        cityRepository.deleteById(id);
    }
}
