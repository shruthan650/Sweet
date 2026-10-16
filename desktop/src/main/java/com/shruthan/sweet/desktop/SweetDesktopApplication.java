package com.shruthan.sweet.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
import java.util.Optional;

public class SweetDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        SweetApiClient apiClient = new SweetApiClient();
        Label statusLabel = new Label("Loading cities...");
        statusLabel.setWrapText(true);
        ListView<CitySummary> cityList = new ListView<>(FXCollections.observableArrayList());
        CityCanvas cityCanvas = new CityCanvas();
        AtomicInteger cityLoadSequence = new AtomicInteger();
        AtomicInteger cityListLoadSequence = new AtomicInteger();
        AtomicBoolean simulationBusy = new AtomicBoolean();
        AtomicBoolean simulationRunning = new AtomicBoolean();
        AtomicReference<String> activeSimulationCityId = new AtomicReference<>();
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
                cityListLoadSequence, null));
        Button createCityButton = new Button("New city");
        Button deleteCityButton = new Button("Delete city");
        deleteCityButton.setDisable(true);
        createCityButton.setOnAction(event -> showCreateCityDialog(stage, apiClient, cityList, cityCanvas,
                statusLabel, refreshButton, cityListLoadSequence));
        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Resume");
        Button tickButton = new Button("Step");
        Button resetButton = new Button("Reset");
        Label simulationLabel = new Label("Simulation idle");
        Timeline simulationTimer = new Timeline();
        simulationTimer.getKeyFrames().setAll(new KeyFrame(Duration.seconds(1), event -> {
            String activeCityId = activeSimulationCityId.get();
            if (activeCityId == null || !simulationBusy.compareAndSet(false, true)) {
                return;
            }
            CitySummary tickingSelection = cityList.getSelectionModel().getSelectedItem();
            if (tickingSelection != null && activeCityId.equals(tickingSelection.id())) {
                updateSimulationButtons(true, false, true,
                        startButton, pauseButton, resumeButton, tickButton, resetButton);
            }
            apiClient.tickSimulation(activeCityId).whenComplete((result, failure) -> Platform.runLater(() -> {
                simulationBusy.set(false);
                if (!activeCityId.equals(activeSimulationCityId.get())) {
                    return;
                }
                if (failure != null) {
                    simulationTimerStop(simulationTimer, activeSimulationCityId, simulationRunning);
                    CitySummary currentSelection = cityList.getSelectionModel().getSelectedItem();
                    if (currentSelection != null && activeCityId.equals(currentSelection.id())) {
                        simulationLabel.setText("Automatic tick failed: " + errorMessage(failure));
                        updateSimulationButtons(false, false, false,
                                startButton, pauseButton, resumeButton, tickButton, resetButton);
                        apiClient.pauseSimulation(activeCityId).whenComplete((paused, pauseFailure) -> {
                            if (pauseFailure != null) {
                                Platform.runLater(() -> simulationLabel.setText(
                                        "Automatic tick failed: " + errorMessage(failure)
                                                + "; pause failed: " + errorMessage(pauseFailure)));
                            }
                        });
                    }
                    return;
                }
                simulationRunning.set(result.running());
                if (!result.running()) {
                    simulationTimerStop(simulationTimer, activeSimulationCityId, simulationRunning);
                }
                CitySummary currentSelection = cityList.getSelectionModel().getSelectedItem();
                if (currentSelection != null && activeCityId.equals(currentSelection.id())) {
                    simulationLabel.setText("Tick " + result.tick() + " · "
                            + (result.running() ? "running" : "paused"));
                    loadCity(apiClient, currentSelection, cityCanvas, currentCity, statusLabel, cityLoadSequence);
                    updateSimulationButtons(false, currentSelection == null, result.running(),
                            startButton, pauseButton, resumeButton,
                            tickButton, resetButton);
                }
            }));
        }));
        simulationTimer.setCycleCount(Animation.INDEFINITE);
        deleteCityButton.setOnAction(event -> confirmDeleteCity(stage, apiClient, cityList, cityCanvas,
                statusLabel, refreshButton, cityListLoadSequence, selectedCity.get(), deleteCityButton,
                simulationTimer, activeSimulationCityId, simulationRunning));
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
                assignDestination(apiClient, selectedCity.get(), currentCity, selectedVehicle.get(),
                        selection, statusLabel, cityCanvas);
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
        saveCityButton.setOnAction(event -> saveCity(apiClient, selectedCity, currentCity, cityCanvas, statusLabel));
        tools.getChildren().add(saveCityButton);
        saveCityButton.setDisable(true);
        HBox simulationControls = new HBox(8, startButton, pauseButton, resumeButton, tickButton, resetButton);
        simulationControls.setPadding(new Insets(10));
        startButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, simulationRunning,
                activeSimulationCityId, simulationTimer, startButton, pauseButton, resumeButton, tickButton, resetButton,
                apiClient::startSimulation));
        pauseButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, simulationRunning,
                activeSimulationCityId, simulationTimer, startButton, pauseButton, resumeButton, tickButton, resetButton,
                apiClient::pauseSimulation));
        resumeButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, simulationRunning,
                activeSimulationCityId, simulationTimer, startButton, pauseButton, resumeButton, tickButton, resetButton,
                apiClient::resumeSimulation));
        tickButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, simulationRunning,
                activeSimulationCityId, simulationTimer, startButton, pauseButton, resumeButton, tickButton, resetButton,
                apiClient::stepSimulation));
        resetButton.setOnAction(event -> runSimulationAction(apiClient, cityList, cityCanvas, statusLabel,
                simulationLabel, currentCity, cityLoadSequence, simulationBusy, simulationRunning,
                activeSimulationCityId, simulationTimer, startButton, pauseButton, resumeButton, tickButton, resetButton,
                apiClient::resetSimulation));
        cityList.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            boolean noCitySelected = selected == null;
            deleteCityButton.setDisable(noCitySelected);
            updateSimulationButtons(simulationBusy.get(), noCitySelected, simulationRunning.get(),
                    startButton, pauseButton, resumeButton, tickButton, resetButton);
            if (selected != null) {
                String activeSimulationId = activeSimulationCityId.get();
                if (activeSimulationId != null && !activeSimulationId.equals(selected.id())) {
                    simulationTimerStop(simulationTimer, activeSimulationCityId, simulationRunning);
                    apiClient.pauseSimulation(activeSimulationId).whenComplete((paused, failure) -> {
                        if (failure != null) {
                            Platform.runLater(() -> simulationLabel.setText(
                                    "Could not pause previous city simulation: " + errorMessage(failure)));
                        }
                    });
                }
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
                updateSimulationButtons(simulationBusy.get(), true, simulationRunning.get(),
                        startButton, pauseButton, resumeButton, tickButton, resetButton);
            }
        });
        startButton.setDisable(true);
        pauseButton.setDisable(true);
        resumeButton.setDisable(true);
        tickButton.setDisable(true);
        resetButton.setDisable(true);

        HBox cityActions = new HBox(8, createCityButton, deleteCityButton);
        VBox sidebar = new VBox(10, new Label("Cities"), refreshButton, cityActions, cityList, statusLabel);
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
        stage.setOnHidden(event -> {
            String activeSimulationId = activeSimulationCityId.getAndSet(null);
            simulationTimer.stop();
            simulationRunning.set(false);
            if (activeSimulationId != null) {
                apiClient.pauseSimulation(activeSimulationId).whenComplete((paused, failure) -> {
                    if (failure != null) {
                        System.err.println("Could not pause simulation on window close: " + errorMessage(failure));
                    }
                });
            }
        });
        stage.show();
        loadInitialCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton, cityListLoadSequence);
    }

    private static void loadInitialCities(SweetApiClient apiClient, ListView<CitySummary> cityList,
            CityCanvas cityCanvas, Label statusLabel, Button refreshButton,
            AtomicInteger cityListLoadSequence) {
        int sequence = cityListLoadSequence.incrementAndGet();
        refreshButton.setDisable(true);
        statusLabel.setText("Checking for saved cities...");
        apiClient.listCities().whenComplete((cities, failure) -> Platform.runLater(() -> {
            if (cityListLoadSequence.get() != sequence) {
                return;
            }
            refreshButton.setDisable(false);
            if (failure != null) {
                statusLabel.setText("Could not load cities: " + errorMessage(failure));
                return;
            }
            boolean demoExists = cities.stream().anyMatch(city -> SampleCity.ID.equals(city.id()));
            if (demoExists) {
                loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton,
                        cityListLoadSequence, SampleCity.ID);
                return;
            }

            boolean hasOlderStarter = cities.stream()
                    .anyMatch(city -> SampleCity.LEGACY_ID.equals(city.id())
                            || SampleCity.PREVIOUS_DEMO_ID.equals(city.id())
                            || SampleCity.OLDER_DEMO_ID.equals(city.id())
                            || SampleCity.OLDEST_DEMO_ID.equals(city.id()));
            if (!cities.isEmpty() && !hasOlderStarter) {
                loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton,
                        cityListLoadSequence, null);
                return;
            }

            statusLabel.setText("Creating the Sweet Traffic Demo...");
            apiClient.createCity(SampleCity.json()).whenComplete((created, createFailure) ->
                    Platform.runLater(() -> {
                        if (cityListLoadSequence.get() != sequence) {
                            return;
                        }
                        if (createFailure != null) {
                            statusLabel.setText("Could not create the traffic demo: "
                                    + errorMessage(createFailure));
                            return;
                        }
                        loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton,
                                cityListLoadSequence, SampleCity.ID);
                    }));
        }));
    }

    private static void loadCities(SweetApiClient apiClient, ListView<CitySummary> cityList, CityCanvas cityCanvas,
            Label statusLabel, Button refreshButton, AtomicInteger cityListLoadSequence, String preferredCityId) {
        int sequence = cityListLoadSequence.incrementAndGet();
        CitySummary previousSelection = cityList.getSelectionModel().getSelectedItem();
        refreshButton.setDisable(true);
        statusLabel.setText("Loading cities...");
        apiClient.listCities().whenComplete((cities, failure) -> Platform.runLater(() -> {
            if (cityListLoadSequence.get() != sequence) {
                return;
            }
            refreshButton.setDisable(false);
            if (failure != null) {
                statusLabel.setText("Could not refresh cities: " + errorMessage(failure));
                return;
            }
            cityList.getItems().setAll(cities);
            if (cities.isEmpty()) {
                cityCanvas.setCity(null);
                cityList.getSelectionModel().clearSelection();
                statusLabel.setText("No cities are available.");
            } else {
                String preferredId = preferredCityId != null ? preferredCityId
                        : previousSelection == null ? null : previousSelection.id();
                int selectedIndex = -1;
                if (preferredId != null) {
                    for (int index = 0; index < cityList.getItems().size(); index++) {
                        if (preferredId.equals(cityList.getItems().get(index).id())) {
                            selectedIndex = index;
                            break;
                        }
                    }
                }
                if (selectedIndex >= 0) {
                    cityList.getSelectionModel().select(selectedIndex);
                } else {
                    cityList.getSelectionModel().selectFirst();
                }
            }
        }));
    }

    private static void showCreateCityDialog(Stage owner, SweetApiClient apiClient, ListView<CitySummary> cityList,
            CityCanvas cityCanvas, Label statusLabel, Button refreshButton,
            AtomicInteger cityListLoadSequence) {
        Dialog<CityDraft> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Create city");
        dialog.setHeaderText("Choose the name and grid dimensions.");
        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("City name");
        TextField widthField = new TextField("20");
        TextField heightField = new TextField("15");
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(10);
        fields.addRow(0, new Label("Name"), nameField);
        fields.addRow(1, new Label("Width"), widthField);
        fields.addRow(2, new Label("Height"), heightField);
        dialog.getDialogPane().setContent(fields);
        dialog.setResultConverter(button -> button == createButton
                ? new CityDraft(nameField.getText(), widthField.getText(), heightField.getText())
                : null);

        Optional<CityDraft> draft = dialog.showAndWait();
        if (draft.isEmpty()) {
            return;
        }
        String name = draft.get().name().trim();
        int width;
        int height;
        try {
            width = Integer.parseInt(draft.get().width().trim());
            height = Integer.parseInt(draft.get().height().trim());
        } catch (NumberFormatException exception) {
            statusLabel.setText("City width and height must be whole numbers.");
            return;
        }
        if (name.isBlank() || width < 5 || width > 40 || height < 5 || height > 40) {
            statusLabel.setText("Enter a city name and grid dimensions between 5 and 40.");
            return;
        }

        statusLabel.setText("Creating " + name + "...");
        apiClient.createCity(name, width, height).whenComplete((created, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                statusLabel.setText("Could not create city: " + errorMessage(failure));
                return;
            }
            loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton, cityListLoadSequence,
                    created.id());
        }));
    }

    private static void confirmDeleteCity(Stage owner, SweetApiClient apiClient, ListView<CitySummary> cityList,
            CityCanvas cityCanvas, Label statusLabel, Button refreshButton,
            AtomicInteger cityListLoadSequence, CitySummary selectedCity, Button deleteCityButton,
            Timeline simulationTimer, AtomicReference<String> activeSimulationCityId,
            AtomicBoolean simulationRunning) {
        if (selectedCity == null) {
            statusLabel.setText("Select a city before deleting it.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selectedCity.name() + "? This cannot be undone.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.initOwner(owner);
        confirmation.setTitle("Delete city");
        confirmation.setHeaderText("Delete city");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        boolean wasRunning = selectedCity.id().equals(activeSimulationCityId.get());
        if (wasRunning) {
            simulationTimerStop(simulationTimer, activeSimulationCityId, simulationRunning);
        }
        deleteCityButton.setDisable(true);
        statusLabel.setText("Deleting " + selectedCity.name() + "...");
        apiClient.deleteCity(selectedCity.id()).whenComplete((ignored, failure) -> Platform.runLater(() -> {
            deleteCityButton.setDisable(cityList.getSelectionModel().getSelectedItem() == null);
            if (failure != null) {
                statusLabel.setText("Could not delete city: " + errorMessage(failure));
                if (wasRunning) {
                    apiClient.resumeSimulation(selectedCity.id()).whenComplete((resumed, resumeFailure) ->
                            Platform.runLater(() -> {
                                if (resumeFailure != null) {
                                    statusLabel.setText("Could not delete city: " + errorMessage(failure)
                                            + "; could not resume simulation: " + errorMessage(resumeFailure));
                                    return;
                                }
                                activeSimulationCityId.set(selectedCity.id());
                                simulationRunning.set(resumed.running());
                                simulationTimer.playFromStart();
                            }));
                }
                return;
            }
            loadCities(apiClient, cityList, cityCanvas, statusLabel, refreshButton, cityListLoadSequence, null);
        }));
    }

    private record CityDraft(String name, String width, String height) {
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

    private static void saveCity(SweetApiClient apiClient, AtomicReference<CitySummary> selectedCity,
            AtomicReference<JsonNode> currentCity,
            CityCanvas cityCanvas, Label statusLabel) {
        CitySummary citySummary = selectedCity.get();
        JsonNode city = currentCity.get();
        if (citySummary == null || city == null) {
            statusLabel.setText("Select a saved city before saving changes.");
            return;
        }
        String submittedSnapshot = city.toString();
        statusLabel.setText("Saving " + citySummary.name() + "...");
        apiClient.updateCity(citySummary.id(), submittedSnapshot).whenComplete((saved, failure) ->
                Platform.runLater(() -> {
                    CitySummary currentSelection = selectedCity.get();
                    if (currentCity.get() != city || currentSelection == null
                            || !citySummary.id().equals(currentSelection.id())) {
                        return;
                    }
                    if (failure != null) {
                        statusLabel.setText("Could not save city: " + errorMessage(failure));
                        return;
                    }
                    cityCanvas.refresh();
                    statusLabel.setText(city.toString().equals(submittedSnapshot)
                            ? "City saved successfully."
                            : "Earlier edits were saved; newer changes still need saving.");
                }));
    }

    private static void runSimulationAction(SweetApiClient apiClient, ListView<CitySummary> cityList,
            CityCanvas cityCanvas, Label cityStatus, Label simulationStatus, AtomicReference<JsonNode> currentCity,
            AtomicInteger cityLoadSequence,
            AtomicBoolean simulationBusy, AtomicBoolean simulationRunning,
            AtomicReference<String> activeSimulationCityId, Timeline simulationTimer,
            Button startButton, Button pauseButton, Button resumeButton,
            Button tickButton, Button resetButton,
            Function<String, java.util.concurrent.CompletableFuture<SimulationStatus>> action) {
        CitySummary selected = cityList.getSelectionModel().getSelectedItem();
        if (selected == null || !simulationBusy.compareAndSet(false, true)) {
            return;
        }
        updateSimulationButtons(true, false, simulationRunning.get(),
                startButton, pauseButton, resumeButton, tickButton, resetButton);
        simulationStatus.setText("Sending simulation command...");
        action.apply(selected.id()).whenComplete((result, failure) -> Platform.runLater(() -> {
            simulationBusy.set(false);
            CitySummary currentSelection = cityList.getSelectionModel().getSelectedItem();
            boolean sameCitySelected = currentSelection != null && selected.id().equals(currentSelection.id());
            if (failure != null) {
                updateSimulationButtons(false, currentSelection == null, simulationRunning.get(),
                        startButton, pauseButton, resumeButton, tickButton, resetButton);
                if (sameCitySelected) {
                    simulationStatus.setText("Simulation request failed: " + errorMessage(failure));
                }
                return;
            }
            if (result.running()) {
                String previousActiveCity = activeSimulationCityId.getAndSet(selected.id());
                if (previousActiveCity != null && !previousActiveCity.equals(selected.id())) {
                    apiClient.pauseSimulation(previousActiveCity).whenComplete((paused, pauseFailure) -> {
                        if (pauseFailure != null) {
                            Platform.runLater(() -> simulationStatus.setText(
                                    "Could not pause previous city simulation: " + errorMessage(pauseFailure)));
                        }
                    });
                }
                simulationRunning.set(true);
                simulationTimer.playFromStart();
            } else if (selected.id().equals(activeSimulationCityId.get())) {
                simulationTimerStop(simulationTimer, activeSimulationCityId, simulationRunning);
            } else {
                simulationRunning.set(false);
            }
            updateSimulationButtons(false, currentSelection == null, simulationRunning.get(),
                    startButton, pauseButton, resumeButton, tickButton, resetButton);
            if (!sameCitySelected) {
                return;
            }
            simulationStatus.setText("Tick " + result.tick() + " · "
                    + (result.running() ? "running" : "paused"));
            loadCity(apiClient, selected, cityCanvas, currentCity, cityStatus, cityLoadSequence);
        }));
    }

    private static void updateSimulationButtons(boolean busy, boolean noCitySelected, boolean running,
            Button startButton, Button pauseButton, Button resumeButton, Button tickButton, Button resetButton) {
        startButton.setDisable(busy || noCitySelected || running);
        pauseButton.setDisable(busy || noCitySelected || !running);
        resumeButton.setDisable(busy || noCitySelected || running);
        tickButton.setDisable(busy || noCitySelected || running);
        resetButton.setDisable(busy || noCitySelected);
    }

    private static void simulationTimerStop(Timeline simulationTimer,
            AtomicReference<String> activeSimulationCityId, AtomicBoolean simulationRunning) {
        simulationTimer.stop();
        activeSimulationCityId.set(null);
        simulationRunning.set(false);
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
            JsonNode intersection = findIntersectionAt(intersections, x, y);
            if (intersection == null) {
                statusLabel.setText("Vehicles must be placed on an intersection.");
                return;
            }
            ObjectNode vehicle = city.objectNode();
            vehicle.put("id", newId("vehicle"));
            vehicle.put("name", "Vehicle " + (vehicles.size() + 1));
            vehicle.put("x", x);
            vehicle.put("y", y);
            vehicle.put("speedKph", 30);
            vehicle.put("status", "IDLE");
            vehicle.set("currentIntersection", minimalIntersection(city, intersection));
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

    private static void assignDestination(SweetApiClient apiClient, CitySummary selectedCity,
            AtomicReference<JsonNode> currentCity, JsonNode vehicle, EntitySelection selection,
            Label statusLabel, CityCanvas cityCanvas) {
        JsonNode cityNode = currentCity.get();
        if (!(cityNode instanceof ObjectNode city) || vehicle == null || selectedCity == null) {
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
        String vehicleId = editableVehicle.path("id").asText();
        String destinationId = destination.path("id").asText();
        statusLabel.setText("Calculating route to " + destination.path("name").asText() + "...");
        apiClient.assignDestination(selectedCity.id(), vehicleId, destinationId)
                .whenComplete((computedRoute, failure) -> Platform.runLater(() -> {
                    if (currentCity.get() != cityNode) {
                        return;
                    }
                    if (failure != null) {
                        statusLabel.setText("Could not assign destination: " + errorMessage(failure));
                        return;
                    }
                    ObjectNode updatedVehicle = findById(array(city, "vehicles"), computedRoute.vehicleId());
                    JsonNode resolvedDestination = findById(array(city, "intersections"),
                            computedRoute.destinationIntersectionId());
                    if (updatedVehicle == null || resolvedDestination == null) {
                        statusLabel.setText("The vehicle or destination is no longer in this city.");
                        return;
                    }
                    updatedVehicle.set("destination", minimalIntersection(city, resolvedDestination));
                    ArrayNode route = city.arrayNode();
                    for (String intersectionId : computedRoute.route()) {
                        JsonNode intersection = findById(array(city, "intersections"), intersectionId);
                        if (intersection != null) {
                            route.add(minimalIntersection(city, intersection));
                        }
                    }
                    updatedVehicle.set("route", route);
                    updatedVehicle.put("status", route.isEmpty() ? "ARRIVED" : "MOVING");
                    statusLabel.setText("Route set to " + resolvedDestination.path("name").asText()
                            + " (" + route.size() + " steps).");
                    cityCanvas.refresh();
                }));
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
        if ("Road".equals(selection.kind())) {
            ObjectNode removedRoad = findById(array(city, "roads"), id);
            array(city, collection).removeIf(entity -> id.equals(entity.path("id").asText()));
            removeRoadLinks(city, id);
            if (removedRoad != null) {
                invalidateRoutesUsingRoad(city, removedRoad);
            }
        } else if ("Intersection".equals(selection.kind())) {
            array(city, collection).removeIf(entity -> id.equals(entity.path("id").asText()));
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
                boolean currentIntersectionRemoved = id.equals(relationId(vehicle.path("currentIntersection")));
                boolean routeUsesIntersection = id.equals(relationId(vehicle.path("destination")))
                        || routeContainsIntersection(vehicle.path("route"), id);
                if (currentIntersectionRemoved) {
                    editableVehicle.putNull("currentIntersection");
                }
                if (currentIntersectionRemoved || routeUsesIntersection) {
                    clearVehicleRoute(city, editableVehicle);
                }
            }
        } else {
            array(city, collection).removeIf(entity -> id.equals(entity.path("id").asText()));
        }
        cityCanvas.clearSelection();
        statusLabel.setText(selection.kind() + " removed.");
        cityCanvas.refresh();
    }

    private static void invalidateRoutesUsingRoad(ObjectNode city, ObjectNode road) {
        String roadStartId = relationId(road.path("start"));
        String roadEndId = relationId(road.path("end"));
        boolean bidirectional = road.path("bidirectional").asBoolean();
        for (JsonNode vehicle : array(city, "vehicles")) {
            ObjectNode editableVehicle = (ObjectNode) vehicle;
            JsonNode currentIntersection = vehicle.path("currentIntersection");
            JsonNode start = findIntersectionAt(array(city, "intersections"),
                    vehicle.path("x").asInt(), vehicle.path("y").asInt());
            String previousId = currentIntersection.isNull() || currentIntersection.isMissingNode()
                    ? start == null ? "" : start.path("id").asText()
                    : relationId(currentIntersection);
            boolean routeUsesRoad = false;
            for (JsonNode next : vehicle.path("route")) {
                String nextId = relationId(next);
                if ((roadStartId.equals(previousId) && roadEndId.equals(nextId))
                        || (bidirectional && roadEndId.equals(previousId) && roadStartId.equals(nextId))) {
                    routeUsesRoad = true;
                    break;
                }
                previousId = nextId;
            }
            if (routeUsesRoad) {
                clearVehicleRoute(city, editableVehicle);
            }
        }
    }

    private static boolean routeContainsIntersection(JsonNode route, String intersectionId) {
        for (JsonNode intersection : route) {
            if (intersectionId.equals(relationId(intersection))) {
                return true;
            }
        }
        return false;
    }

    private static void clearVehicleRoute(ObjectNode city, ObjectNode vehicle) {
        vehicle.putNull("destination");
        vehicle.set("route", city.arrayNode());
        vehicle.put("status", "IDLE");
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
            case "vehicle" -> "Click an intersection to place a vehicle.";
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
