package com.shruthan.sweet.service;

public class CityNotFoundException extends IllegalArgumentException {

    public CityNotFoundException(String id) {
        super("City not found: " + id);
    }
}
