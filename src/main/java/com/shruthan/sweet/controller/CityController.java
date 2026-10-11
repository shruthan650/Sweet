package com.shruthan.sweet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.service.CityService;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public List<City> getCities() {
        return cityService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<City> getCity(@PathVariable String id) {
        return cityService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<City> createCity(@RequestBody City city) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.create(city));
    }

    @PutMapping("/{id}")
    public ResponseEntity<City> updateCity(@PathVariable String id, @RequestBody City city) {
        return ResponseEntity.ok(cityService.update(id, city));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable String id) {
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
