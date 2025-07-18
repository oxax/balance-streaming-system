package com.jpc.exercise.audit.application;

import com.jpc.exercise.audit.domain.model.AuditBatch;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AuditStatsService {

    private final List<AuditBatch> recentBatches = new CopyOnWriteArrayList<>();
    private final List<String> telemetryEvents = new CopyOnWriteArrayList<>();
    private final AtomicInteger totalBatches = new AtomicInteger();
    private final AtomicInteger totalTransactions = new AtomicInteger();

    public AuditStatsService() {
    }

    public void record(List<AuditBatch> batches) {
        recentBatches.addAll(batches);
        totalBatches.addAndGet(batches.size());
        batches.forEach(batch -> totalTransactions.addAndGet(batch.getTransactionCount()));
    }

    public void recordTelemetryEvent(String message) {
        telemetryEvents.add(String.format("[%s] %s", Instant.now(), message));
    }

    public List<AuditBatch> getRecentBatches() {
        return List.copyOf(recentBatches);
    }

    public Stats getStatsSnapshot() {
        return new Stats(totalBatches.get(), totalTransactions.get());
    }

    public List<String> getTelemetryEvents() {
        return List.copyOf(telemetryEvents);
    }

    public record Stats(int totalBatches, int totalTransactions) {}
}