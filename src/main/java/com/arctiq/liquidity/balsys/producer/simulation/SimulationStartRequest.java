package com.arctiq.liquidity.balsys.producer.simulation;

public class SimulationStartRequest {
    private int transactionCount = 1000; // default
    private int durationSeconds = 10; // default

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
