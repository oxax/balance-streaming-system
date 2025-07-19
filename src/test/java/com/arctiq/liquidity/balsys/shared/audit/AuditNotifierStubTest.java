package com.arctiq.liquidity.balsys.shared.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AuditNotifierStubTest {

    private RandomGenerator generator;
    private MetricsCollector metricsCollector;
    private AuditNotifier stub;

    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault();
        metricsCollector = new MetricsCollector(new SimpleMeterRegistry());
        stub = new ConsoleAuditNotifier(metricsCollector, new AuditStatsService());
    }

    @Test
    void shouldLogBatchesToConsole() {
        List<AuditBatch> mockBatches = new ArrayList<>();

        Transaction tx1 = new Transaction(
                TransactionId.generate(),
                Money.of(-100_000.0),
                Instant.now());

        Transaction tx2 = new Transaction(
                TransactionId.generate(),
                Money.of(50_000.0),
                Instant.now());

        mockBatches.add(new AuditBatch("batch-1", List.of(tx1, tx2)));

        stub.submit(mockBatches);
    }
}
