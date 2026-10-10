package com.shruthan.sweet.model;

public enum TrafficLightState {
    RED,
    GREEN,
    YELLOW;

    public TrafficLightState nextState() {
        return switch (this) {
            case RED -> GREEN;
            case GREEN -> YELLOW;
            case YELLOW -> RED;
        };
    }

    public boolean canTransitionTo(TrafficLightState nextState) {
        if (nextState == null) {
            return false;
        }

        return switch (this) {
            case RED -> nextState == GREEN;
            case GREEN -> nextState == YELLOW;
            case YELLOW -> nextState == RED;
        };
    }
}
