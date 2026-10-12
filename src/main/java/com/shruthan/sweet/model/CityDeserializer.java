package com.shruthan.sweet.model;

import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

public class CityDeserializer extends ValueDeserializer<City> {

    @Override
    public City deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        JsonNode node = parser.readValueAsTree();
        int width = node.path("width").asInt(10);
        int height = node.path("height").asInt(10);

        return new City(
                node.path("id").asString("city-1"),
                node.path("name").asString("Sweet"),
                width,
                height,
                readList(node.get("roads"), Road.class, context),
                readList(node.get("intersections"), Intersection.class, context),
                readList(node.get("vehicles"), Vehicle.class, context),
                readList(node.get("buildings"), Building.class, context));
    }

    private <T> List<T> readList(JsonNode node, Class<T> itemType, DeserializationContext context)
            throws JacksonException {
        if (node == null || node.isNull()) {
            return List.of();
        }
        JavaType listType = context.getTypeFactory().constructCollectionType(List.class, itemType);
        return context.readTreeAsValue(node, listType);
    }
}
