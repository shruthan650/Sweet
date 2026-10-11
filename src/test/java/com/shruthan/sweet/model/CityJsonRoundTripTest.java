package com.shruthan.sweet.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class CityJsonRoundTripTest {

    @Test
    void roundTripKeepsGridDimensionsAheadOfBuildings() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
        City city = new City("city-1", "Downtown", 24, 16);
        city.addBuilding(new Building("building-1", "Tower", 10, 6, 2, 2, BuildingType.RESIDENTIAL));

        City restored = mapper.readValue(mapper.writeValueAsBytes(city), City.class);

        assertEquals(24, restored.getWidth());
        assertEquals(16, restored.getHeight());
        assertEquals(1, restored.getBuildings().size());
        assertEquals(10, restored.getBuildings().getFirst().getX());
    }
}
