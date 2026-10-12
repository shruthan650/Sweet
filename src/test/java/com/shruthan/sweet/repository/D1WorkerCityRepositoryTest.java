package com.shruthan.sweet.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;

import com.shruthan.sweet.model.City;
import com.shruthan.sweet.model.Intersection;
import com.shruthan.sweet.model.Road;
import com.shruthan.sweet.model.RoadType;
import com.shruthan.sweet.model.TrafficLight;
import com.shruthan.sweet.model.TrafficLightState;

class D1WorkerCityRepositoryTest {

    private static final String SECRET = "worker-test-secret-0123456789-abcdef";

    @Test
    void createSendsFlattenedCityWithWorkerAuthenticationAndLoadsResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        D1WorkerCityRepository repository = new D1WorkerCityRepository(builder, "https://worker.test", SECRET);
        String responseBody = """
                {
                  "id":"city-1","name":"Downtown","width":20,"height":20,
                  "roads":[],"intersections":[],"vehicles":[],"buildings":[]
                }
                """;
        server.expect(requestTo("https://worker.test/v1/cities"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SECRET))
                .andExpect(MockRestRequestMatchers.content().string(org.hamcrest.Matchers.containsString("\"start\":\"i1\"")))
                .andExpect(MockRestRequestMatchers.content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("\"intersection\":{\"id\":\"i1\""))))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        City city = cityWithConnectedRoad();
        City saved = repository.create(city);

        assertEquals("city-1", saved.getId());
        assertEquals("Downtown", saved.getName());
        server.verify();
    }

    @Test
    void missingCityIsRepresentedAsAnEmptyOptional() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        D1WorkerCityRepository repository = new D1WorkerCityRepository(builder, "https://worker.test", SECRET);
        server.expect(requestTo("https://worker.test/v1/cities/does-not-exist"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(repository.findById("does-not-exist").isEmpty());
        server.verify();
    }

    @Test
    void persistenceCallsFailClosedWhenSecretIsMissing() {
        D1WorkerCityRepository repository = new D1WorkerCityRepository(
                RestClient.builder(), "https://worker.test", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class, repository::findAll);

        assertTrue(exception.getMessage().contains("SWEET_WORKER_SHARED_SECRET"));
    }

    @Test
    void nonLocalWorkerMustUseHttps() {
        assertThrows(IllegalArgumentException.class,
                () -> new D1WorkerCityRepository(RestClient.builder(), "http://worker.test", SECRET));
    }

    private City cityWithConnectedRoad() {
        City city = new City("city-1", "Downtown", 20, 20);
        Intersection start = new Intersection("i1", 2, 2);
        Intersection end = new Intersection("i2", 8, 2);
        start.setTrafficLight(new TrafficLight("light-1", start, TrafficLightState.RED, 20));
        city.addRoad(new Road("r1", "Main Street", start, end, RoadType.STREET));
        return city;
    }
}
