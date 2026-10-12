package com.shruthan.sweet.dto;

import java.util.List;

public record SimulationStatusDto(String cityId, long tick, boolean running, List<String> events) {
}
