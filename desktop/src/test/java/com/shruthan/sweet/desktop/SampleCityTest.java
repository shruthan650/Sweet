package com.shruthan.sweet.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SampleCityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sampleCityHasConnectedTrafficRoutesAndWorkingSignalSeeds() throws Exception {
        JsonNode city = mapper.readTree(SampleCity.json());

        assertEquals("sweet-traffic-demo-v5", city.path("id").asText());
        assertEquals(16, city.path("intersections").size());
        assertEquals(24, city.path("roads").size());
        assertEquals(12, city.path("buildings").size());
        assertEquals(20, city.path("vehicles").size());

        int lights = 0;
        for (JsonNode intersection : city.path("intersections")) {
            if (!intersection.path("trafficLight").isNull()) {
                lights++;
            }
        }
        assertEquals(4, lights);

        for (JsonNode vehicle : city.path("vehicles")) {
            String startId = vehicle.path("currentIntersection").path("id").asText();
            String destinationId = vehicle.path("destination").path("id").asText();
            assertNotEquals(startId, destinationId, vehicle.path("id").asText());
            assertFalse(vehicle.path("route").isEmpty(), vehicle.path("id").asText());
            boolean routeVisitsDestination = false;
            for (JsonNode waypoint : vehicle.path("route")) {
                if (destinationId.equals(waypoint.path("id").asText())) {
                    routeVisitsDestination = true;
                    break;
                }
            }
            assertTrue(routeVisitsDestination, vehicle.path("id").asText());
        }
    }
}
