package com.shruthan.sweet.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class SweetApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private static final TypeReference<List<CitySummary>> CITY_LIST_TYPE = new TypeReference<>() {
    };

    private final HttpClient httpClient;
    private final URI baseUri;

    public SweetApiClient() {
        this(URI.create(System.getenv().getOrDefault("SWEET_API_BASE_URL", DEFAULT_BASE_URL)),
                HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    SweetApiClient(URI baseUri, HttpClient httpClient) {
        this.baseUri = validateBaseUri(baseUri);
        this.httpClient = httpClient;
    }

    public CompletableFuture<List<CitySummary>> listCities() {
        return send("GET", "/api/cities").thenApply(body -> {
            try {
                List<CitySummary> cities = OBJECT_MAPPER.readValue(body, CITY_LIST_TYPE);
                if (cities == null) {
                    throw new IllegalStateException("Sweet API returned an empty city list response");
                }
                return cities;
            } catch (IOException exception) {
                throw new CompletionException("Sweet API returned an invalid city list response", exception);
            }
        });
    }

    public CompletableFuture<String> getCity(String cityId) {
        return send("GET", "/api/cities/" + encodePathSegment(cityId));
    }

    public CompletableFuture<com.fasterxml.jackson.databind.JsonNode> getCityDetails(String cityId) {
        return send("GET", "/api/cities/" + encodePathSegment(cityId)).thenApply(body -> {
            try {
                return OBJECT_MAPPER.readTree(body);
            } catch (IOException exception) {
                throw new CompletionException("Sweet API returned an invalid city response", exception);
            }
        });
    }

    public CompletableFuture<String> createCity(String cityJson) {
        return sendJson("POST", "/api/cities", cityJson);
    }

    public CompletableFuture<String> updateCity(String cityId, String cityJson) {
        return sendJson("PUT", "/api/cities/" + encodePathSegment(cityId), cityJson);
    }

    public CompletableFuture<Void> deleteCity(String cityId) {
        return send("DELETE", "/api/cities/" + encodePathSegment(cityId)).thenApply(response -> null);
    }

    public CompletableFuture<SimulationStatus> startSimulation(String cityId) {
        return simulationAction(cityId, "start");
    }

    public CompletableFuture<SimulationStatus> pauseSimulation(String cityId) {
        return simulationAction(cityId, "pause");
    }

    public CompletableFuture<SimulationStatus> resumeSimulation(String cityId) {
        return simulationAction(cityId, "resume");
    }

    public CompletableFuture<SimulationStatus> tickSimulation(String cityId) {
        return simulationAction(cityId, "tick");
    }

    public CompletableFuture<SimulationStatus> resetSimulation(String cityId) {
        return simulationAction(cityId, "reset");
    }

    public CompletableFuture<VehicleRoute> assignDestination(
            String cityId, String vehicleId, String destinationIntersectionId) {
        if (destinationIntersectionId == null || destinationIntersectionId.isBlank()) {
            throw new IllegalArgumentException("Destination intersection id is required");
        }
        String requestBody;
        try {
            requestBody = OBJECT_MAPPER.writeValueAsString(new DestinationRequest(destinationIntersectionId));
        } catch (IOException exception) {
            throw new CompletionException("Could not serialize destination request", exception);
        }
        String path = "/api/simulation/cities/" + encodePathSegment(cityId)
                + "/vehicles/" + encodePathSegment(vehicleId) + "/destination";
        return sendJson("PUT", path, requestBody).thenApply(body -> {
            try {
                return OBJECT_MAPPER.readValue(body, VehicleRoute.class);
            } catch (IOException exception) {
                throw new CompletionException("Sweet API returned an invalid vehicle route response", exception);
            }
        });
    }

    private CompletableFuture<SimulationStatus> simulationAction(String cityId, String action) {
        return send("POST", "/api/simulation/cities/" + encodePathSegment(cityId) + "/" + action)
                .thenApply(body -> {
                    try {
                        return OBJECT_MAPPER.readValue(body, SimulationStatus.class);
                    } catch (IOException exception) {
                        throw new CompletionException("Sweet API returned an invalid simulation response", exception);
                    }
                });
    }

    private CompletableFuture<String> send(String method, String path) {
        return send(method, path, null);
    }

    private CompletableFuture<String> sendJson(String method, String path, String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) {
            throw new IllegalArgumentException("JSON request body is required");
        }
        return send(method, path, jsonBody);
    }

    private CompletableFuture<String> send(String method, String path, String jsonBody) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
        if (jsonBody == null) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        HttpRequest request = requestBuilder.build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new ApiException(response.statusCode(), response.body());
                    }
                    return response.body();
                });
    }

    private static URI validateBaseUri(URI uri) {
        if (uri == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
            throw new IllegalArgumentException("API base URL must be an HTTP(S) origin without a path or credentials");
        }
        return uri;
    }

    private static String encodePathSegment(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("City id is required");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static final class ApiException extends RuntimeException {

        private final int statusCode;

        public ApiException(int statusCode, String responseBody) {
            super("Sweet API returned HTTP " + statusCode
                    + (responseBody == null || responseBody.isBlank() ? "" : ": " + responseBody));
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    private record DestinationRequest(String destinationIntersectionId) {
    }

    public record VehicleRoute(String vehicleId, String destinationIntersectionId, List<String> route) {
    }
}
