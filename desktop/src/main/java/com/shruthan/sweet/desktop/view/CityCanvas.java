package com.shruthan.sweet.desktop.view;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

public final class CityCanvas extends Canvas {

    private static final double CELL_SIZE = 34;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 3;

    private JsonNode city;
    private double zoom = 1;
    private double panX;
    private double panY;
    private double dragX;
    private double dragY;
    private double originX;
    private double originY;
    private double scale;
    private String selectedKind;
    private String selectedId;
    private String tool = "select";
    private Consumer<EntitySelection> selectionHandler = ignored -> {
    };
    private BiConsumer<Integer, Integer> gridClickHandler = (x, y) -> {
    };

    public CityCanvas() {
        widthProperty().addListener(ignored -> draw());
        heightProperty().addListener(ignored -> draw());
        setOnMousePressed(this::startPan);
        setOnMouseDragged(this::pan);
        setOnMouseClicked(this::selectEntity);
        setOnScroll(this::changeZoom);
        draw();
    }

    public void setSelectionHandler(Consumer<EntitySelection> selectionHandler) {
        this.selectionHandler = selectionHandler == null ? ignored -> {
        } : selectionHandler;
    }

    public void setGridClickHandler(BiConsumer<Integer, Integer> gridClickHandler) {
        this.gridClickHandler = gridClickHandler == null ? (x, y) -> {
        } : gridClickHandler;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public void refresh() {
        draw();
    }

    public void clearSelection() {
        selectedKind = null;
        selectedId = null;
        selectionHandler.accept(null);
        draw();
    }

    public void setCity(JsonNode city) {
        this.city = city;
        zoom = 1;
        panX = 0;
        panY = 0;
        selectedKind = null;
        selectedId = null;
        draw();
    }

    private void startPan(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
            dragX = event.getX() - panX;
            dragY = event.getY() - panY;
        }
    }

    private void pan(MouseEvent event) {
        if (event.isPrimaryButtonDown() || event.isMiddleButtonDown()) {
            panX = event.getX() - dragX;
            panY = event.getY() - dragY;
            draw();
        }
    }

    private void changeZoom(ScrollEvent event) {
        if (city == null) {
            return;
        }
        double nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * (event.getDeltaY() > 0 ? 1.1 : 0.9)));
        if (nextZoom == zoom) {
            return;
        }
        int cityWidth = city.path("width").asInt();
        int cityHeight = city.path("height").asInt();
        double worldWidth = cityWidth * CELL_SIZE;
        double worldHeight = cityHeight * CELL_SIZE;
        double fitScale = Math.min(getWidth() / worldWidth, getHeight() / worldHeight);
        double currentScale = fitScale * zoom;
        double nextScale = fitScale * nextZoom;
        double worldX = (event.getX() - originX) / currentScale;
        double worldY = (event.getY() - originY) / currentScale;
        panX = event.getX() - (getWidth() - worldWidth * nextScale) / 2 - worldX * nextScale;
        panY = event.getY() - (getHeight() - worldHeight * nextScale) / 2 - worldY * nextScale;
        zoom = nextZoom;
        draw();
        event.consume();
    }

    private void draw() {
        GraphicsContext graphics = getGraphicsContext2D();
        double canvasWidth = getWidth();
        double canvasHeight = getHeight();
        graphics.setFill(Color.web("#101b29"));
        graphics.fillRect(0, 0, canvasWidth, canvasHeight);

        if (city == null) {
            graphics.setFill(Color.web("#a9b7c9"));
            graphics.fillText("Select a city to view its map.", 24, 36);
            return;
        }

        int cityWidth = city.path("width").asInt();
        int cityHeight = city.path("height").asInt();
        if (cityWidth <= 0 || cityHeight <= 0) {
            graphics.setFill(Color.web("#ff9b91"));
            graphics.fillText("The selected city has invalid dimensions.", 24, 36);
            return;
        }

        double worldWidth = cityWidth * CELL_SIZE;
        double worldHeight = cityHeight * CELL_SIZE;
        double fitScale = Math.min(canvasWidth / worldWidth, canvasHeight / worldHeight);
        if (!Double.isFinite(fitScale) || fitScale <= 0) {
            return;
        }
        scale = fitScale * zoom;
        originX = (canvasWidth - worldWidth * scale) / 2 + panX;
        originY = (canvasHeight - worldHeight * scale) / 2 + panY;

        graphics.save();
        graphics.translate(originX, originY);
        graphics.scale(scale, scale);
        drawGrid(graphics, cityWidth, cityHeight);
        drawRoads(graphics, city);
        drawBuildings(graphics, city);
        drawIntersections(graphics, city);
        drawVehicles(graphics, city);
        graphics.restore();
    }

    private void selectEntity(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || city == null || scale <= 0) {
            return;
        }
        double x = (event.getX() - originX) / scale;
        double y = (event.getY() - originY) / scale;
        double gridX = x / CELL_SIZE;
        double gridY = y / CELL_SIZE;
        int cellX = (int) Math.floor(gridX);
        int cellY = (int) Math.floor(gridY);
        if (cellX < 0 || cellY < 0 || cellX >= city.path("width").asInt()
                || cellY >= city.path("height").asInt()) {
            return;
        }
        if ("intersection".equals(tool) || "building".equals(tool) || "vehicle".equals(tool)) {
            gridClickHandler.accept(cellX, cellY);
        } else {
            EntitySelection selection = findEntity(x, y);
            if ("road".equals(tool) && (selection == null || !"Intersection".equals(selection.kind()))) {
                selection = null;
            }
            if ("destination".equals(tool)
                    && (selection == null || !"Intersection".equals(selection.kind()))) {
                selection = null;
            }
            selectedKind = selection == null ? null : selection.kind();
            selectedId = selection == null ? null : selection.entity().path("id").asText();
            selectionHandler.accept(selection);
        }
        draw();
    }

    private EntitySelection findEntity(double x, double y) {
        for (JsonNode vehicle : city.path("vehicles")) {
            if (distance(x, y, (vehicle.path("x").asDouble() + 0.5) * CELL_SIZE,
                    (vehicle.path("y").asDouble() + 0.5) * CELL_SIZE) <= CELL_SIZE * 0.4) {
                return new EntitySelection("Vehicle", vehicle);
            }
        }
        for (JsonNode intersection : city.path("intersections")) {
            if (distance(x, y, (intersection.path("x").asDouble() + 0.5) * CELL_SIZE,
                    (intersection.path("y").asDouble() + 0.5) * CELL_SIZE) <= CELL_SIZE * 0.4) {
                return new EntitySelection("Intersection", intersection);
            }
        }
        for (JsonNode building : city.path("buildings")) {
            double left = building.path("x").asDouble() * CELL_SIZE;
            double top = building.path("y").asDouble() * CELL_SIZE;
            double right = left + building.path("width").asDouble() * CELL_SIZE;
            double bottom = top + building.path("height").asDouble() * CELL_SIZE;
            if (x >= left && x <= right && y >= top && y <= bottom) {
                return new EntitySelection("Building", building);
            }
        }
        for (JsonNode road : city.path("roads")) {
            JsonNode start = road.path("start");
            JsonNode end = road.path("end");
            if (!start.has("x") || !start.has("y") || !end.has("x") || !end.has("y")) {
                continue;
            }
            double startX = (start.path("x").asDouble() + 0.5) * CELL_SIZE;
            double startY = (start.path("y").asDouble() + 0.5) * CELL_SIZE;
            double endX = (end.path("x").asDouble() + 0.5) * CELL_SIZE;
            double endY = (end.path("y").asDouble() + 0.5) * CELL_SIZE;
            if (distanceToSegment(x, y, startX, startY, endX, endY) <= CELL_SIZE * 0.35) {
                return new EntitySelection("Road", road);
            }
        }
        return null;
    }

    private static void drawGrid(GraphicsContext graphics, int width, int height) {
        double gridWidth = width * CELL_SIZE;
        double gridHeight = height * CELL_SIZE;
        graphics.setFill(Color.web("#101b29"));
        graphics.fillRect(0, 0, gridWidth, gridHeight);
        graphics.setStroke(Color.web("#29384a"));
        graphics.setLineWidth(1);
        for (int x = 0; x <= width; x++) {
            graphics.strokeLine(x * CELL_SIZE, 0, x * CELL_SIZE, gridHeight);
        }
        for (int y = 0; y <= height; y++) {
            graphics.strokeLine(0, y * CELL_SIZE, gridWidth, y * CELL_SIZE);
        }
    }

    private void drawRoads(GraphicsContext graphics, JsonNode city) {
        for (JsonNode road : city.path("roads")) {
            JsonNode start = road.path("start");
            JsonNode end = road.path("end");
            if (!start.has("x") || !start.has("y") || !end.has("x") || !end.has("y")) {
                continue;
            }
            double startX = (start.path("x").asDouble() + 0.5) * CELL_SIZE;
            double startY = (start.path("y").asDouble() + 0.5) * CELL_SIZE;
            double endX = (end.path("x").asDouble() + 0.5) * CELL_SIZE;
            double endY = (end.path("y").asDouble() + 0.5) * CELL_SIZE;
            graphics.setStroke(Color.web("#25374b"));
            graphics.setLineWidth(20);
            graphics.strokeLine(startX, startY, endX, endY);
            graphics.setStroke(Color.web("#617187"));
            graphics.setLineWidth(14);
            graphics.strokeLine(startX, startY, endX, endY);
            if (isSelected("Road", road)) {
                graphics.setStroke(Color.web("#75c8ff"));
                graphics.setLineWidth(16);
                graphics.strokeLine(startX, startY, endX, endY);
            }
            graphics.setStroke(Color.web("#c2a873"));
            graphics.setLineWidth(1);
            graphics.setLineDashes(5, 5);
            graphics.strokeLine(startX, startY, endX, endY);
            graphics.setLineDashes();
        }
    }

    private void drawBuildings(GraphicsContext graphics, JsonNode city) {
        for (JsonNode building : city.path("buildings")) {
            double x = building.path("x").asDouble() * CELL_SIZE + 4;
            double y = building.path("y").asDouble() * CELL_SIZE + 4;
            double width = Math.max(building.path("width").asDouble() * CELL_SIZE - 8, 8);
            double height = Math.max(building.path("height").asDouble() * CELL_SIZE - 8, 8);
            graphics.setFill(Color.web("#547b76"));
            graphics.fillRoundRect(x, y, width, height, 10, 10);
            graphics.setStroke(Color.web("#91b0a8"));
            graphics.setLineWidth(1.5);
            graphics.strokeRoundRect(x, y, width, height, 10, 10);
            if (isSelected("Building", building)) {
                graphics.setStroke(Color.web("#75c8ff"));
                graphics.setLineWidth(3);
                graphics.strokeRoundRect(x, y, width, height, 10, 10);
            }
        }
    }

    private void drawIntersections(GraphicsContext graphics, JsonNode city) {
        for (JsonNode intersection : city.path("intersections")) {
            double x = (intersection.path("x").asDouble() + 0.5) * CELL_SIZE;
            double y = (intersection.path("y").asDouble() + 0.5) * CELL_SIZE;
            graphics.setFill(Color.web("#d9e3eb"));
            graphics.fillOval(x - 6, y - 6, 12, 12);
            if (isSelected("Intersection", intersection)) {
                graphics.setStroke(Color.web("#75c8ff"));
                graphics.setLineWidth(2);
                graphics.strokeOval(x - 12, y - 12, 24, 24);
            }
            JsonNode light = intersection.path("trafficLight");
            if (light.isObject()) {
                Color color = switch (light.path("state").asText("")) {
                    case "GREEN" -> Color.web("#65d98e");
                    case "YELLOW" -> Color.web("#f4c95d");
                    default -> Color.web("#f27676");
                };
                graphics.setFill(color);
                graphics.fillOval(x + 5, y - 13, 8, 8);
            }
        }
    }

    private void drawVehicles(GraphicsContext graphics, JsonNode city) {
        for (JsonNode vehicle : city.path("vehicles")) {
            double x = (vehicle.path("x").asDouble() + 0.5) * CELL_SIZE;
            double y = (vehicle.path("y").asDouble() + 0.5) * CELL_SIZE;
            graphics.setFill(Color.web("#72b7ff"));
            graphics.fillRoundRect(x - 8, y - 5, 16, 10, 5, 5);
            graphics.setStroke(Color.web("#e4f2ff"));
            graphics.setLineWidth(1);
            graphics.strokeRoundRect(x - 8, y - 5, 16, 10, 5, 5);
            if (isSelected("Vehicle", vehicle)) {
                graphics.setStroke(Color.web("#75c8ff"));
                graphics.setLineWidth(2);
                graphics.strokeOval(x - 12, y - 12, 24, 24);
            }
        }
    }

    private boolean isSelected(String kind, JsonNode entity) {
        return kind.equals(selectedKind) && entity.path("id").asText().equals(selectedId);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private static double distanceToSegment(double x, double y, double startX, double startY,
            double endX, double endY) {
        double segmentX = endX - startX;
        double segmentY = endY - startY;
        double lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared == 0) {
            return distance(x, y, startX, startY);
        }
        double projection = Math.max(0, Math.min(1,
                ((x - startX) * segmentX + (y - startY) * segmentY) / lengthSquared));
        return distance(x, y, startX + projection * segmentX, startY + projection * segmentY);
    }
}
