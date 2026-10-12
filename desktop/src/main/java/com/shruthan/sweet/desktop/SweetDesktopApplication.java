package com.shruthan.sweet.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.shruthan.sweet.desktop.api.CitySummary;
import com.shruthan.sweet.desktop.api.SimulationStatus;
import com.shruthan.sweet.desktop.api.SweetApiClient;
import com.shruthan.sweet.desktop.view.CityCanvas;
import com.shruthan.sweet.desktop.view.EntitySelection;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.UUID;

public class SweetDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        SweetApiClient apiClient = new SweetApiClient();
        Label statusLabel = new Label("Loading cities...");
        statusLabel.setWrapText(true);
        ListView<CitySummary> cityList = new ListView<>(FXCollections.observableArrayList());
        CityCanvas cityCanvas = new CityCanvas();
        AtomicInteger cityLoadSequence = new AtomicInteger();
        AtomicBoolean simulationBusy = new AtomicBoolean();
        AtomicReference<JsonNode> currentCity = new AtomicReference<>();
        AtomicReference<String> currentTool = new AtomicReference<>("select");
        AtomicReference<JsonNode> roadStart = new AtomicReference<>();
        AtomicReference<JsonNode> selectedVehicle = new AtomicReference<>();
        AtomicReference<CitySummary> selectedCity = new AtomicReference<>();
        cityList.setPlaceholder(new Label("No cities found"));
        cityList.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(CitySummary city, boolean empty) {
                super.updateItem(city, empty);
                setText(empty || city == null
                        ? null
                        : "%s (%d x %d)".formatted(city.name(), city.width(), city.height()));
            }
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        refreshButton.setOnAction(event -> loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton,
                cityLoadSequence));
        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Resume");
        Button tickButton = new Button("Step");
        Button resetButton = new Button("Reset");
        Label simulationLabel = new Label("Simulation idle");
        Label inspectorLabel = new Label("Select a map feature to inspect it.");
        inspectorLabel.setWrapText(true);
        VBox inspector = new VBox(10, new Label("Inspector"), inspectorLabel);
        inspector.setPadding(new Insets(16));
        inspector.setPrefWidth(230);
        cityCanvas.setSelectionHandler(selection -> {
            showSelection(selection, inspectorLabel);
            if (!"destination".equals(currentTool.get())) {
                selectedVehicle.set(selection != null && "Vehicle".equals(selection.kind())
                        ? selection.entity() : null);
            }
            if ("erase".equals(currentTool.get()) && selection != null) {
                eraseEntity(currentCity.get(), selection, statusLabel, cityCanvas);
                if ("Vehicle".equals(selection.kind())) {
                    selectedVehicle.set(null);
                }
            } else if ("road".equals(currentTool.get()) && selection != null) {
                connectRoad(currentCity.get(), selection, roadStart, statusLabel, cityCanvas);
            } else if ("destination".equals(currentTool.get()) && selection != null) {
                assignDestination(currentCity.get(), selectedVehicle.get(), selection, statusLabel, cityCanvas);
            }
        });
        cityCanvas.setGridClickHandler((x, y) -> placeEntity(currentCity.get(), currentTool.get(),
                x, y, cityCanvas, statusLabel));
        HBox tools = new HBox(8);
        for (String tool : new String[] { "select", "intersection", "road", "building", "vehicle", "destination", "erase" }) {
            Button toolButton = new Button(tool.substring(0, 1).toUpperCase() + tool.substring(1));
            toolButton.setOnAction(event -> {
                currentTool.set(tool);
                roadStart.set(null);
                cityCanvas.setTool(tool);
                statusLabel.setText(toolInstructions(tool));
            });
            tools.getChildren().add(toolButton);
        }
        Button saveCityButton = new Button("Save city");
        saveCityButton.setOnAction(event -> saveCity(apiClient, selectedCity.get(), currentCity.get(), cityCanvas,
                statusLabel));
        tools.getChildren().add(saveCityButton);
        saveCityButton.setDisable(true);
        HBox simulationControls = new HBox(8, startButton, pauseButton, resumeButton, tickButton, resetButton);
        simulationControls.setPadding(new Insets(10));
        startButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, startButton, pauseButton, resumeButton,
                tickButton, resetButton, apiClient::startSimulation));
        pauseButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, startButton, pauseButton, resumeButton,
                tickButton, resetButton, apiClient::pauseSimulation));
        resumeButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, startButton, pauseButton, resumeButton,
                tickButton, resetButton, apiClient::resumeSimulation));
        tickButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, startButton, pauseButton, resumeButton,
                tickButton, resetButton, apiClient::tickSimulation));
        resetButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, startButton, pauseButton, resumeButton,
                tickButton, resetButton, apiClient::resetSimulation));
        cityList.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            boolean noCitySelected = selected == null;
            startButton.setDisable(noCitySelected || simulationBusy.get());
            pauseButton.setDisable(noCitySelected || simulationBusy.get());
            resumeButton.setDisable(noCitySelected || simulationBusy.get());
            tickButton.setDisable(noCitySelected || simulationBusy.get());
            resetButton.setDisable(noCitySelected || simulationBusy.get());
            if (selected != null) {
                selectedCity.set(selected);
                selectedVehicle.set(null);
                roadStart.set(null);
                currentTool.set("select");
                cityCanvas.setTool("select");
                inspectorLabel.setText("Select a map feature to inspect it.");
                saveCityButton.setDisable(false);
                loadCity(apiClient, selected, cityCanvas, currentCity, statusLabel, cityLoadSequence);
                simulationLabel.setText("Simulation controls for " + selected.name());
            } else {
                selectedCity.set(null);
                currentCity.set(null);
                selectedVehicle.set(null);
                roadStart.set(null);
                saveCityButton.setDisable(true);
            }
        });
        startButton.setDisable(true);
        pauseButton.setDisable(true);
        resumeButton.setDisable(true);
        tickButton.setDisable(true);
        resetButton.setDisable(true);

        VBox sidebar = new VBox(10, new Label("Cities"), refreshButton, cityList, statusLabel);
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(280);
        VBox.setVgrow(cityList, Priority.ALWAYS);

        StackPane canvasPane = new StackPane(cityCanvas);
        cityCanvas.widthProperty().bind(canvasPane.widthProperty());
        cityCanvas.heightProperty().bind(canvasPane.heightProperty());
        VBox mapContent = new VBox(canvasPane, simulationControls, simulationLabel);
        mapContent.getChildren().add(0, tools);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);
        BorderPane layout = new BorderPane(mapContent);
        layout.setLeft(sidebar);
        layout.setRight(inspector);
        Scene scene = new Scene(layout, 1000, 700);
        stage.setTitle("Sweet");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
        loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton, cityLoadSequence);
    }

    private static void loadCities(SweetApiClient apiClient, ListView<CitySummary> cityList, CityCanvas cityCanvas,
            Label statusLabel, Button refreshButton, AtomicInteger cityLoadSequence) {
        cityLoadSequence.incrementAndGet();
        refreshButton.setDisable(true);
        statusLabel.setText("Loading cities...");
        apiClient.listCities().whenComplete((cities, failure) -> Platform.runLater(() -> {
            refreshButton.setDisable(false);
            if (failure != null) {
                cityList.getItems().clear();
                cityCanvas.setCity(null);
                statusLabel.setText("Could not load cities: " + errorMessage(failure));
                return;
            }
            cityList.getItems().setAll(cities);
            if (cities.isEmpty()) {
                cityCanvas.setCity(null);
                statusLabel.setText("No cities are available.");
            } else {
                cityList.getSelectionModel().selectFirst();
            }
        }));
    }

    private static void loadCity(SweetApiClient apiClient, CitySummary city, CityCanvas cityCanvas,
            AtomicReference<JsonNode> currentCity, Label statusLabel, AtomicInteger cityLoadSequence) {
        int sequence = cityLoadSequence.incrementAndGet();
        statusLabel.setText("Loading " + city.name() + "...");
        apiClient.getCityDetails(city.id()).whenComplete((details, failure) -> Platform.runLater(() -> {
            if (cityLoadSequence.get() != sequence) {
                return;
            }
            if (failure != null) {
                currentCity.set(null);
                cityCanvas.setCity(null);
                statusLabel.setText("Could not load " + city.name() + ": " + errorMessage(failure));
                return;
            }
            JsonNode editableCity = details.deepCopy();
            currentCity.set(editableCity);
            cityCanvas.setCity(editableCity);
            statusLabel.setText(city.name() + " · " + city.width() + " x " + city.height());
        }));
    }

    private static void saveCity(SweetApiClient apiClient, CitySummary selectedCity, JsonNode city,
            CityCanvas cityCanvas, Label statusLabel) {
        if (selectedCity == null || city == null) {
            statusLabel.setText("Select a saved city before saving changes.");
            return;
        }
        statusLabel.setText("Saving " + selectedCity.name() + "...");
        apiClient.updateCity(selectedCity.id(), city.toString()).whenComplete((saved, failure) ->
                Platform.runLater(() -> {
                    if (failure != null) {
                        statusLabel.setText("Could not save city: " + errorMessage(failure));
                        return;
                    }
                    cityCanvas.refresh();
                    statusLabel.setText("City saved successfully.");
                }));
    }

    private static void runSimulationAction(SweetApiClient apiClient, ListView<CitySummary> cityList,
            CityCanvas cityCanvas, Label cityStatus, Label simulationStatus, AtomicReference<JsonNode> currentCity,
            AtomicInteger cityLoadSequence,
            AtomicBoolean simulationBusy, Button startButton, Button pauseButton, Button resumeButton,
            Button tickButton, Button resetButton, Function<String, java.util.concurrent.CompletableFuture<SimulationStatus>> action) {
        CitySummary selected = cityList.getSelectionModel().getSelectedItem();
        if (selected == null || !simulationBusy.compareAndSet(false, true)) {
            return;
        }
        setSimulationButtonsDisabled(true, startButton, pauseButton, resumeButton, tickButton, resetButton);
        simulationStatus.setText("Sending simulation command...");
        action.apply(selected.id()).whenComplete((result, failure) -> Platform.runLater(() -> {
            simulationBusy.set(false);
            CitySummary currentSelection = cityList.getSelectionModel().getSelectedItem();
            boolean sameCitySelected = currentSelection != null && selected.id().equals(currentSelection.id());
            setSimulationButtonsDisabled(currentSelection == null, startButton, pauseButton, resumeButton, tickButton,
                    resetButton);
            if (!sameCitySelected) {
                return;
            }
            if (failure != null) {
                simulationStatus.setText("Simulation request failed: " + errorMessage(failure));
                return;
            }
            simulationStatus.setText("Tick " + result.tick() + " · "
                    + (result.running() ? "running" : "paused"));
            loadCity(apiClient, selected, cityCanvas, currentCity, cityStatus, cityLoadSequence);
        }));
    }

    private static void setSimulationButtonsDisabled(boolean disabled, Button startButton, Button pauseButton,
            Button resumeButton, Button tickButton, Button resetButton) {
        startButton.setDisable(disabled);
        pauseButton.setDisable(disabled);
        resumeButton.setDisable(disabled);
        tickButton.setDisable(disabled);
        resetButton.setDisable(disabled);
    }

    private static void showSelection(EntitySelection selection, Label inspectorLabel) {
        if (selection == null) {
            inspectorLabel.setText("Select a map feature to inspect it.");
            return;
        }
        var entity = selection.entity();
        StringBuilder details = new StringBuilder(selection.kind())
                .append(": ")
                .append(entity.path("name").asText("Unnamed"))
                .append("\nID: ")
                .append(entity.path("id").asText("unknown"));
        if (entity.has("x") && entity.has("y")) {
            details.append("\nPosition: ")
                    .append(entity.path("x").asInt())
                    .append(", ")
                    .append(entity.path("y").asInt());
        }
        for (String field : new String[] { "type", "status", "state" }) {
            if (entity.hasNonNull(field)) {
                details.append("\n")
                        .append(field.substring(0, 1).toUpperCase())
                        .append(field.substring(1))
                        .append(": ")
                        .append(entity.path(field).asText());
            }
        }
        inspectorLabel.setText(details.toString());
    }

    private static void placeEntity(JsonNode cityNode, String tool, int x, int y,
            CityCanvas cityCanvas, Label statusLabel) {
        if (!(cityNode instanceof ObjectNode city)) {
            statusLabel.setText("Select a city before editing the map.");
            return;
        }
        ArrayNode intersections = array(city, "intersections");
        if ("intersection".equals(tool)) {
            if (containsCell(intersections, x, y)) {
                statusLabel.setText("An intersection already occupies that cell.");
                return;
            }
            ObjectNode intersection = city.objectNode();
            intersection.put("id", newId("intersection"));
            intersection.put("name", "Intersection " + (intersections.size() + 1));
            intersection.put("x", x);
            intersection.put("y", y);
            intersection.set("incomingRoads", city.arrayNode());
            intersection.set("outgoingRoads", city.arrayNode());
            intersection.putNull("trafficLight");
            intersections.add(intersection);
            statusLabel.setText("Intersection placed at " + x + ", " + y + ".");
        } else if ("building".equals(tool)) {
            ArrayNode buildings = array(city, "buildings");
            boolean overlaps = false;
            for (JsonNode building : buildings) {
                if (x < building.path("x").asInt() + building.path("width").asInt()
                        && x + 1 > building.path("x").asInt()
                        && y < building.path("y").asInt() + building.path("height").asInt()
                        && y + 1 > building.path("y").asInt()) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) {
                statusLabel.setText("Buildings cannot overlap.");
                return;
            }
            ObjectNode building = city.objectNode();
            building.put("id", newId("building"));
            building.put("name", "Building " + (buildings.size() + 1));
            building.put("x", x);
            building.put("y", y);
            building.put("width", 1);
            building.put("height", 1);
            building.put("type", "RESIDENTIAL");
            buildings.add(building);
            statusLabel.setText("Building placed at " + x + ", " + y + ".");
        } else if ("vehicle".equals(tool)) {
            ArrayNode vehicles = array(city, "vehicles");
            ObjectNode vehicle = city.objectNode();
            vehicle.put("id", newId("vehicle"));
            vehicle.put("name", "Vehicle " + (vehicles.size() + 1));
            vehicle.put("x", x);
            vehicle.put("y", y);
            vehicle.put("speedKph", 30);
            vehicle.put("status", "IDLE");
            JsonNode intersection = findIntersectionAt(intersections, x, y);
            if (intersection == null) {
                vehicle.putNull("currentIntersection");
            } else {
                vehicle.set("currentIntersection", minimalIntersection(city, intersection));
            }
            vehicle.set("route", city.arrayNode());
            vehicle.putNull("destination");
            vehicles.add(vehicle);
            statusLabel.setText("Vehicle placed at " + x + ", " + y + ".");
        } else {
            statusLabel.setText(toolInstructions(tool));
            return;
        }
        cityCanvas.refresh();
    }

    private static void connectRoad(JsonNode cityNode, EntitySelection selection,
            AtomicReference<JsonNode> roadStart, Label statusLabel, CityCanvas cityCanvas) {
        if (!(cityNode instanceof ObjectNode city) || !"Intersection".equals(selection.kind())) {
            statusLabel.setText("Roads must connect two intersections.");
            roadStart.set(null);
            return;
        }
        JsonNode end = selection.entity();
        JsonNode start = roadStart.get();
        if (start == null) {
            roadStart.set(end);
            statusLabel.setText("Road start selected. Choose a second intersection.");
            return;
        }
        if (start.path("id").asText().equals(end.path("id").asText())) {
            statusLabel.setText("Choose a different intersection for the road endpoint.");
            return;
        }
        ArrayNode roads = array(city, "roads");
        for (JsonNode road : roads) {
            String roadStartId = relationId(road.path("start"));
            String roadEndId = relationId(road.path("end"));
            if ((roadStartId.equals(start.path("id").asText()) && roadEndId.equals(end.path("id").asText()))
                    || (roadStartId.equals(end.path("id").asText())
                            && roadEndId.equals(start.path("id").asText()))) {
                statusLabel.setText("A road already connects those intersections.");
                roadStart.set(null);
                return;
            }
        }
        int dx = end.path("x").asInt() - start.path("x").asInt();
        int dy = end.path("y").asInt() - start.path("y").asInt();
        ObjectNode road = city.objectNode();
        String roadId = newId("road");
        road.put("id", roadId);
        road.put("name", "Road " + (roads.size() + 1));
        road.set("start", minimalIntersection(city, start));
        road.set("end", minimalIntersection(city, end));
        road.put("type", "STREET");
        road.put("direction", dx == 0 ? "VERTICAL" : dy == 0 ? "HORIZONTAL" : "DIAGONAL");
        road.put("bidirectional", true);
        road.put("length", Math.max(Math.abs(dx), Math.abs(dy)));
        roads.add(road);
        addRoadLink(city, start.path("id").asText(), "outgoingRoads", roadId);
        addRoadLink(city, end.path("id").asText(), "incomingRoads", roadId);
        addRoadLink(city, end.path("id").asText(), "outgoingRoads", roadId);
        addRoadLink(city, start.path("id").asText(), "incomingRoads", roadId);
        roadStart.set(null);
        statusLabel.setText("Road connected " + start.path("name").asText()
                + " to " + end.path("name").asText() + ".");
        cityCanvas.refresh();
    }

    private static void assignDestination(JsonNode cityNode, JsonNode vehicle, EntitySelection selection,
            Label statusLabel, CityCanvas cityCanvas) {
        if (!(cityNode instanceof ObjectNode city) || vehicle == null) {
            statusLabel.setText("Select a vehicle first, then choose the Destination tool.");
            return;
        }
        if (!"Intersection".equals(selection.kind())) {
            statusLabel.setText("Choose an intersection as the vehicle destination.");
            return;
        }
        ObjectNode editableVehicle = findById(array(city, "vehicles"), vehicle.path("id").asText());
        if (editableVehicle == null) {
            statusLabel.setText("The selected vehicle is no longer in this city.");
            return;
        }
        JsonNode destination = selection.entity();
        editableVehicle.set("destination", minimalIntersection(city, destination));
        ArrayNode route = city.arrayNode();
        route.add(minimalIntersection(city, destination));
        editableVehicle.set("route", route);
        editableVehicle.put("status", "MOVING");
        statusLabel.setText("Destination set to " + destination.path("name").asText() + ".");
        cityCanvas.refresh();
    }

    private static void eraseEntity(JsonNode cityNode, EntitySelection selection,
            Label statusLabel, CityCanvas cityCanvas) {
        if (!(cityNode instanceof ObjectNode city)) {
            return;
        }
        String id = selection.entity().path("id").asText();
        String collection = switch (selection.kind()) {
            case "Road" -> "roads";
            case "Intersection" -> "intersections";
            case "Building" -> "buildings";
            case "Vehicle" -> "vehicles";
            default -> null;
        };
        if (collection == null) {
            return;
        }
        array(city, collection).removeIf(entity -> id.equals(entity.path("id").asText()));
        if ("Road".equals(selection.kind())) {
            removeRoadLinks(city, id);
        } else if ("Intersection".equals(selection.kind())) {
            ArrayNode roads = array(city, "roads");
            for (int index = roads.size() - 1; index >= 0; index--) {
                JsonNode road = roads.get(index);
                if (id.equals(relationId(road.path("start"))) || id.equals(relationId(road.path("end")))) {
                    String roadId = road.path("id").asText();
                    roads.remove(index);
                    removeRoadLinks(city, roadId);
                }
            }
            for (JsonNode vehicle : array(city, "vehicles")) {
                ObjectNode editableVehicle = (ObjectNode) vehicle;
                if (id.equals(relationId(vehicle.path("currentIntersection")))) {
                    editableVehicle.putNull("currentIntersection");
                }
                if (id.equals(relationId(vehicle.path("destination")))) {
                    editableVehicle.putNull("destination");
                    editableVehicle.set("route", city.arrayNode());
                    editableVehicle.put("status", "IDLE");
                }
            }
        }
        cityCanvas.clearSelection();
        statusLabel.setText(selection.kind() + " removed.");
        cityCanvas.refresh();
    }

    private static ArrayNode array(ObjectNode city, String field) {
        JsonNode value = city.path(field);
        if (value instanceof ArrayNode existing) {
            return existing;
        }
        ArrayNode created = city.arrayNode();
        city.set(field, created);
        return created;
    }

    private static boolean containsCell(ArrayNode intersections, int x, int y) {
        return findIntersectionAt(intersections, x, y) != null;
    }

    private static JsonNode findIntersectionAt(ArrayNode intersections, int x, int y) {
        for (JsonNode intersection : intersections) {
            if (intersection.path("x").asInt() == x && intersection.path("y").asInt() == y) {
                return intersection;
            }
        }
        return null;
    }

    private static ObjectNode minimalIntersection(ObjectNode city, JsonNode intersection) {
        ObjectNode reference = city.objectNode();
        reference.put("id", intersection.path("id").asText());
        reference.put("name", intersection.path("name").asText());
        reference.put("x", intersection.path("x").asInt());
        reference.put("y", intersection.path("y").asInt());
        reference.set("incomingRoads", city.arrayNode());
        reference.set("outgoingRoads", city.arrayNode());
        JsonNode trafficLight = intersection.path("trafficLight");
        reference.set("trafficLight", trafficLight.isObject() ? trafficLight.deepCopy() : city.nullNode());
        return reference;
    }

    private static void addRoadLink(ObjectNode city, String intersectionId, String field, String roadId) {
        for (JsonNode intersection : array(city, "intersections")) {
            if (!intersectionId.equals(intersection.path("id").asText())) {
                continue;
            }
            ArrayNode links = array((ObjectNode) intersection, field);
            ObjectNode link = city.objectNode();
            link.put("id", roadId);
            links.add(link);
            return;
        }
    }

    private static void removeRoadLinks(ObjectNode city, String roadId) {
        for (JsonNode intersection : array(city, "intersections")) {
            ObjectNode editableIntersection = (ObjectNode) intersection;
            for (String field : new String[] { "incomingRoads", "outgoingRoads" }) {
                array(editableIntersection, field)
                        .removeIf(road -> roadId.equals(road.isTextual() ? road.asText() : road.path("id").asText()));
            }
        }
    }

    private static ObjectNode findById(ArrayNode entities, String id) {
        for (JsonNode entity : entities) {
            if (id.equals(entity.path("id").asText())) {
                return (ObjectNode) entity;
            }
        }
        return null;
    }

    private static String relationId(JsonNode relation) {
        return relation.isTextual() ? relation.asText() : relation.path("id").asText();
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String toolInstructions(String tool) {
        return switch (tool) {
            case "intersection" -> "Click an empty grid cell to place an intersection.";
            case "road" -> "Select two intersections to connect with a road.";
            case "building" -> "Click an empty grid cell to place a building.";
            case "vehicle" -> "Click a grid cell to place a vehicle.";
            case "destination" -> "Select a vehicle, then click an intersection.";
            case "erase" -> "Click a map feature to remove it.";
            default -> "Click a map feature to inspect it.";
        };
    }

    private static String errorMessage(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
