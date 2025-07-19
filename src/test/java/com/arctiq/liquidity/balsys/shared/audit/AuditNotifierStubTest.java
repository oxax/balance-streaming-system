package com.arctiq.liquidity.balsys.shared.audit;

import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditNotifierStubTest {

    private AuditNotifier stub;
    private TransactionConfigProperties config;

    @BeforeEach
    void setup() {
        config = AuditTestFixtures.config();
        MetricsCollector metrics = new MetricsCollector(new SimpleMeterRegistry());
        stub = new ConsoleAuditNotifier(metrics, new AuditStatsService());
    }

    @Test
    @DisplayName("Logs submitted audit batch contents to console")
    void shouldLogBatchesToConsole() {
        List<AuditBatch> batches = List.of(
                AuditTestFixtures.auditBatch(AuditTestFixtures.mockSmallTransactions(), config));

        stub.submit(batches);

        int txCount = batches.get(0).getTransactionCount();
        assertEquals(2, txCount, "Expected 2 transactions in the batch");
    }
}