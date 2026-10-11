package com.shruthan.sweet.simulation;

public class SimulationClock {

    private long currentTick;
    private double speedMultiplier;
    private boolean running;

    public SimulationClock() {
        this(0, 1.0d, false);
    }

    public SimulationClock(long currentTick, double speedMultiplier, boolean running) {
        setCurrentTick(currentTick);
        setSpeedMultiplier(speedMultiplier);
        setRunning(running);
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(long currentTick) {
        if (currentTick < 0) {
            throw new IllegalArgumentException("Current tick cannot be negative");
        }
        this.currentTick = currentTick;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Simulation speed must be positive");
        }
        this.speedMultiplier = speedMultiplier;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void tick() {
        currentTick++;
    }

    public void start() {
        this.running = true;
    }

    public void pause() {
        this.running = false;
    }

    public void resume() {
        this.running = true;
    }

    public void reset() {
        this.currentTick = 0;
        this.running = false;
    }
}
