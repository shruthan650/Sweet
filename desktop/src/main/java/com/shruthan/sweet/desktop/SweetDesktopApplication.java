package com.shruthan.sweet.desktop;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.shruthan.sweet.desktop.api.CitySummary;
import com.shruthan.sweet.desktop.api.SweetApiClient;

import java.util.concurrent.CompletionException;

public class SweetDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        SweetApiClient apiClient = new SweetApiClient();
        Label statusLabel = new Label("Loading cities...");
        statusLabel.setWrapText(true);
        ListView<CitySummary> cityList = new ListView<>(FXCollections.observableArrayList());
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
        refreshButton.setOnAction(event -> loadCities(apiClient, cityList, statusLabel, refreshButton));

        VBox sidebar = new VBox(10, new Label("Cities"), refreshButton, cityList, statusLabel);
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(280);
        VBox.setVgrow(cityList, Priority.ALWAYS);

        Label content = new Label("Select a city to get started.");
        BorderPane layout = new BorderPane(content);
        layout.setLeft(sidebar);
        Scene scene = new Scene(layout, 1000, 700);
        stage.setTitle("Sweet");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
        loadCities(apiClient, cityList, statusLabel, refreshButton);
    }

    private static void loadCities(SweetApiClient apiClient, ListView<CitySummary> cityList,
            Label statusLabel, Button refreshButton) {
        refreshButton.setDisable(true);
        statusLabel.setText("Loading cities...");
        apiClient.listCities().whenComplete((cities, failure) -> Platform.runLater(() -> {
            refreshButton.setDisable(false);
            if (failure != null) {
                cityList.getItems().clear();
                statusLabel.setText("Could not load cities: " + errorMessage(failure));
                return;
            }
            cityList.getItems().setAll(cities);
            statusLabel.setText(cities.isEmpty() ? "No cities are available." : "");
        }));
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
