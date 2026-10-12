package com.shruthan.sweet.desktop.api;

import java.util.List;

public record SimulationStatus(String cityId, long tick, boolean running, List<String> events) {
}
