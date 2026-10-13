package com.shruthan.sweet.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.event.Event;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

class CityCanvasTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void clickingAnIntersectionReportsItsSelection() throws Exception {
        CityCanvas canvas = canvasWithCity("""
                {"width":10,"height":10,"intersections":[{"id":"i1","x":2,"y":3}],
                 "roads":[],"buildings":[],"vehicles":[]}
                """);
        AtomicReference<EntitySelection> selected = new AtomicReference<>();
        canvas.setSelectionHandler(selected::set);

        click(canvas, 2.5 * 34, 3.5 * 34);

        assertNotNull(selected.get());
        assertEquals("Intersection", selected.get().kind());
        assertEquals("i1", selected.get().entity().path("id").asText());
    }

    @Test
    void creationToolReportsTheClickedGridCell() throws Exception {
        CityCanvas canvas = canvasWithCity("""
                {"width":10,"height":10,"intersections":[],"roads":[],"buildings":[],"vehicles":[]}
                """);
        AtomicReference<String> clickedCell = new AtomicReference<>();
        canvas.setTool("building");
        canvas.setGridClickHandler((x, y) -> clickedCell.set(x + "," + y));

        click(canvas, 4.5 * 34, 5.5 * 34);

        assertEquals("4,5", clickedCell.get());
    }

    private CityCanvas canvasWithCity(String json) throws Exception {
        CityCanvas canvas = new CityCanvas();
        canvas.setWidth(340);
        canvas.setHeight(340);
        JsonNode city = objectMapper.readTree(json);
        canvas.setCity(city);
        return canvas;
    }

    private void click(CityCanvas canvas, double x, double y) {
        MouseEvent event = new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y,
                MouseButton.PRIMARY, 1, false, false, false, false,
                false, false, false, false, false, true, null);
        Event.fireEvent(canvas, event);
    }
}
