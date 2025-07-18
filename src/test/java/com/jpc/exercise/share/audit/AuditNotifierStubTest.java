package com.jpc.exercise.share.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.application.AuditStatsService;
import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.audit.infrastructure.notifier.ConsoleAuditNotifier;
import com.jpc.exercise.shared.audit.AuditNotifier;
import com.jpc.exercise.shared.observability.MetricsCollector;

class AuditNotifierStubTest {

    private RandomGenerator generator;
    private MetricsCollector metricsCollector;
    private AuditNotifier stub;
    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault();
        metricsCollector = new MetricsCollector(); // Initialize metrics collector
        stub = new ConsoleAuditNotifier(metricsCollector, new AuditStatsService());
    }

    @Test
    void shouldLogBatchesToConsole() {
        List<AuditBatch> mockBatches = new ArrayList<>();

        // Add dummy batches with mock transactions
        mockBatches.add(new AuditBatch("batch-1", List.of(
            new Transaction(generator, -100_000L),
            new Transaction(generator, 50_000L))));

        stub.submit(mockBatches);
    }
}
