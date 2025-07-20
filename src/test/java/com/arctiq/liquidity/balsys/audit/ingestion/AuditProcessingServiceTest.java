package com.arctiq.liquidity.balsys.audit.ingestion;

import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.GreedyBatchingStrategy;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

import static org.junit.jupiter.api.Assertions.*;

class AuditProcessingServiceTest {

    private AuditProcessingService auditService;
    private LoggingAuditNotifier notifier;
    private TransactionConfigProperties config;
    private BlockingQueue<Transaction> transactionQueue;

    @BeforeEach
    void setup() {
        config = AuditTestFixtures.config();
        MetricsCollector metrics = new MetricsCollector(new SimpleMeterRegistry());
        notifier = new LoggingAuditNotifier();
        transactionQueue = new LinkedTransferQueue<>();
        AuditBatchPersistence persistence = new InMemoryAuditBatchStore();
        BatchingStrategy batchingStrategy = new GreedyBatchingStrategy(Money.of(config.getMaxBatchValue()));
        auditService = new AuditProcessingService(transactionQueue, config, batchingStrategy, notifier, persistence,
                metrics);
    }

    @Test
    @DisplayName("Should batch and submit 1000 transactions respecting £1M batch limit")
    void shouldTriggerSubmissionOnExactSubmissionSize() throws InterruptedException {
        List<Transaction> transactions = AuditTestFixtures.randomTransactions(config.getSubmissionLimit(), config);
        for (Transaction tx : transactions) {
            auditService.ingest(tx);
        }

        auditService.flushIfThresholdMet();

        int maxWaitMillis = 1000;
        int elapsed = 0;
        while (elapsed < maxWaitMillis) {
            Thread.sleep(50);
            elapsed += 50;
        }

        int totalSubmittedTx = notifier.submittedBatches.stream()
                .mapToInt(AuditBatch::getTransactionCount)
                .sum();

        assertFalse(notifier.submittedBatches.isEmpty());
        assertEquals(config.getSubmissionLimit(), totalSubmittedTx);

        for (AuditBatch batch : notifier.submittedBatches) {
            assertTrue(batch.getTotalValue().asDouble() <= config.getMaxBatchValue());
        }
    }

    private static class LoggingAuditNotifier implements AuditNotifier {
        List<AuditBatch> submittedBatches = new ArrayList<>();

        @Override
        public void submit(List<AuditBatch> batches) {
            submittedBatches.addAll(batches);
        }
    }
}