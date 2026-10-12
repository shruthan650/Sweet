package com.shruthan.sweet.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SweetDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new Label("Sweet City Simulator"), 1000, 700);
        stage.setTitle("Sweet");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
