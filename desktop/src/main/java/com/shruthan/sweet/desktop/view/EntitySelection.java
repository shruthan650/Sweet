package com.shruthan.sweet.desktop.view;

import com.fasterxml.jackson.databind.JsonNode;

public record EntitySelection(String kind, JsonNode entity) {
}
