package com.shruthan.sweet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.shruthan.sweet.model.City;

@Repository
public interface CityRepository extends MongoRepository<City, String> {
}
