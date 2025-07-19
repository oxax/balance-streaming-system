package com.arctiq.liquidity.balsys.audit.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingAlgorithm;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AuditProcessingServiceTest {

    private static final int SUBMISSION_SIZE = 1_000;
    private static final double MIN_AMOUNT = 200.0;
    private static final double MAX_AMOUNT = 500_000.0;

    private AuditProcessingService auditService;
    private LinkedTransferQueue<Transaction> queue;
    private LoggingAuditNotifier notifier;
    private RandomGenerator generator;
    private AuditBatchPersistence persistence;
    private MetricsCollector metricsCollector;

    @BeforeEach
    void setup() {
        queue = new LinkedTransferQueue<>();
        generator = RandomGenerator.getDefault();
        notifier = new LoggingAuditNotifier();
        persistence = new InMemoryAuditBatchStore();
        metricsCollector = new MetricsCollector(new SimpleMeterRegistry());

        auditService = new AuditProcessingService(
                1000,
                1000,
                1000,
                1,
                new BatchingAlgorithm(new TransactionConfigProperties()),
                notifier,
                persistence,
                metricsCollector);
    }

    @Test
    @DisplayName("Should batch and submit 1000 transactions respecting £1M batch limit")
    void shouldTriggerSubmissionOnExactSubmissionSize() {
        for (int i = 0; i < SUBMISSION_SIZE; i++) {
            queue.offer(generateRandomTransaction());
        }

        auditService.flushIfThresholdMet();

        int totalSubmittedTx = notifier.submittedBatches.stream()
                .mapToInt(batch -> batch.getTransactions().size())
                .sum();

        assertFalse(notifier.submittedBatches.isEmpty(), "No batches submitted");
        assertEquals(SUBMISSION_SIZE, totalSubmittedTx, "Mismatch in submitted transaction count");

        for (AuditBatch batch : notifier.submittedBatches) {
            double batchTotal = batch.getTransactions().stream()
                    .mapToDouble(tx -> tx.absoluteValue().asDouble())
                    .sum();
            assertTrue(batchTotal <= 1_000_000.0, "Batch violates max £1M constraint");
        }
    }

    private Transaction generateRandomTransaction() {
        double value = MIN_AMOUNT + generator.nextDouble() * (MAX_AMOUNT - MIN_AMOUNT);
        boolean isCredit = generator.nextBoolean();
        double signed = isCredit ? value : -value;
        return new Transaction(TransactionId.generate(), Money.of(signed));
    }

    private static class LoggingAuditNotifier implements AuditNotifier {
        List<AuditBatch> submittedBatches = new ArrayList<>();

        @Override
        public void submit(List<AuditBatch> batches) {
            submittedBatches.addAll(batches);
            System.out.println("Audit Submission Report:");
            for (AuditBatch batch : batches) {
                double totalValue = batch.getTransactions().stream()
                        .mapToDouble(tx -> tx.absoluteValue().asDouble())
                        .sum();
                System.out.printf(" - Batch ID: %s | Tx Count: %d | Total Value: £%,.2f%n",
                        batch.getBatchId(), batch.getTransactionCount(), totalValue);
            }
        }
    }
}
