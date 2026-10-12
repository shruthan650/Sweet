package com.shruthan.sweet.desktop.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SweetApiClientTest {

    private HttpServer server;
    private SweetApiClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        client = new SweetApiClient(baseUri, HttpClient.newHttpClient());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void listCitiesDeserializesSummariesFromTheBackendResponse() throws IOException {
        server.createContext("/api/cities", exchange -> respond(exchange, 200,
                """
                [{"id":"city-1","name":"Downtown","width":20,"height":15,"roads":[]}]
                """));

        List<CitySummary> cities = client.listCities().join();

        assertEquals(List.of(new CitySummary("city-1", "Downtown", 20, 15)), cities);
    }

    @Test
    void getCityDetailsReturnsTheCityJsonForCanvasRendering() throws IOException {
        server.createContext("/api/cities/city-1", exchange -> respond(exchange, 200,
                """
                {"id":"city-1","name":"Downtown","width":20,"height":15,
                 "roads":[],"intersections":[],"vehicles":[],"buildings":[]}
                """));

        JsonNode city = client.getCityDetails("city-1").join();

        assertEquals("Downtown", city.path("name").asText());
        assertEquals(20, city.path("width").asInt());
    }

    @Test
    void startSimulationPostsToTheBackendAndDeserializesStatus() throws IOException {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        server.createContext("/api/simulation/cities/city-1/start", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            respond(exchange, 200,
                    """
                    {"cityId":"city-1","tick":3,"running":true,"events":["event-1"]}
                    """);
        });

        SimulationStatus status = client.startSimulation("city-1").join();

        assertEquals("POST", requestMethod.get());
        assertEquals(new SimulationStatus("city-1", 3, true, List.of("event-1")), status);
    }

    @Test
    void assignDestinationPutsIntersectionIdAndReturnsTheComputedRoute() throws IOException {
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/api/simulation/cities/city-1/vehicles/vehicle-1/destination", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
            respond(exchange, 200,
                    """
                    {"vehicleId":"vehicle-1","destinationIntersectionId":"intersection-3",
                     "route":["intersection-2","intersection-3"]}
                    """);
        });

        SweetApiClient.VehicleRoute route = client
                .assignDestination("city-1", "vehicle-1", "intersection-3").join();

        assertEquals("PUT", requestMethod.get());
        assertEquals("{\"destinationIntersectionId\":\"intersection-3\"}", requestBody.get());
        assertEquals(new SweetApiClient.VehicleRoute("vehicle-1", "intersection-3",
                List.of("intersection-2", "intersection-3")), route);
    }

    @Test
    void updateCityPutsJsonToTheSelectedCityRoute() throws IOException {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/api/cities/city-1", exchange -> {
            method.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"id\":\"city-1\",\"name\":\"Updated\"}");
        });

        client.updateCity("city-1", "{\"id\":\"city-1\",\"name\":\"Updated\"}").join();

        assertEquals("PUT", method.get());
        assertEquals("{\"id\":\"city-1\",\"name\":\"Updated\"}", requestBody.get());
    }

    @Test
    void failedResponsesExposeTheirHttpStatusAndBody() throws IOException {
        server.createContext("/api/cities", exchange -> respond(exchange, 503, "{\"error\":\"Unavailable\"}"));

        CompletionException failure = assertThrows(CompletionException.class, () -> client.listCities().join());

        SweetApiClient.ApiException apiFailure = assertInstanceOf(SweetApiClient.ApiException.class,
                failure.getCause());
        assertEquals(503, apiFailure.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(apiFailure.getMessage().contains("Unavailable"));
    }

    @Test
    void cityIdsAreEncodedAndNoContentDeletesCompleteSuccessfully() throws IOException {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        server.createContext("/api/cities", exchange -> {
            requestedPath.set(exchange.getRequestURI().getRawPath());
            respond(exchange, 204, "");
        });

        assertNull(client.deleteCity("city 1/2").join());

        assertEquals("/api/cities/city%201%2F2", requestedPath.get());
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
