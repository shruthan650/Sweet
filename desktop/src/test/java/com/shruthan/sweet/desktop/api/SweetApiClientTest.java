package com.shruthan.sweet.desktop.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
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
