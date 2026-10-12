package com.shruthan.sweet.dto;

import java.util.List;

public record VehicleRouteDto(String vehicleId, String destinationIntersectionId, List<String> route) {
}
