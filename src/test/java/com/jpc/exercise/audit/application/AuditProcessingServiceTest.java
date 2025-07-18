package com.jpc.exercise.audit.application;

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

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.audit.domain.service.BatchingAlgorithm;
import com.jpc.exercise.audit.infrastructure.persistence.AuditBatchPersistence;
import com.jpc.exercise.audit.infrastructure.persistence.InMemoryAuditBatchStore;
import com.jpc.exercise.shared.audit.AuditNotifier;

class AuditProcessingServiceTest {

    private static final int SUBMISSION_SIZE = 1_000;
    private static final long MIN_AMOUNT = 200L;
    private static final long MAX_AMOUNT = 500_000L;

    private AuditProcessingService auditService;
    private LinkedTransferQueue<Transaction> queue;
    private LoggingAuditNotifier notifier;
    private RandomGenerator generator;
    private AuditBatchPersistence persistence;

    @BeforeEach
    void setup() {
        queue = new LinkedTransferQueue<>();
        generator = RandomGenerator.getDefault();
        notifier = new LoggingAuditNotifier(); // now logs batch details
        persistence = new InMemoryAuditBatchStore();
        auditService = new AuditProcessingService(queue, new BatchingAlgorithm(), notifier, persistence);
    }

    @Test
    @DisplayName("Should batch and submit 1000 transactions respecting £1M batch limit")
    void shouldTriggerSubmissionOnExactSubmissionSize() {
        for (int i = 0; i < SUBMISSION_SIZE; i++) {
            queue.offer(generateRandomTransaction());
        }

        auditService.trySubmitIfThresholdMet();

        int totalSubmittedTx = notifier.submittedBatches.stream()
                                    .mapToInt(batch -> batch.getTransactions().size())
                                    .sum();

        assertFalse(notifier.submittedBatches.isEmpty(), "No batches submitted");
        assertEquals(SUBMISSION_SIZE, totalSubmittedTx, "Mismatch in submitted transaction count");

        for (AuditBatch batch : notifier.submittedBatches) {
            long batchTotal = batch.getTransactions().stream()
                                   .mapToLong(tx -> Math.abs(tx.getAmount()))
                                   .sum();
            assertTrue(batchTotal <= 1_000_000L, "Batch violates max £1M constraint");
        }
    }

    private Transaction generateRandomTransaction() {
        long amount = MIN_AMOUNT + (long) (generator.nextDouble() * (MAX_AMOUNT - MIN_AMOUNT));
        boolean isCredit = generator.nextBoolean();
        long signedAmount = isCredit ? amount : -amount;
        return new Transaction(generator, signedAmount);
    }

    /**
     * Notifier that captures and prints batch metadata for observability.
     */
    private static class LoggingAuditNotifier implements AuditNotifier {
        List<AuditBatch> submittedBatches = new ArrayList<>();

        @Override
        public void submit(List<AuditBatch> batches) {
            submittedBatches.addAll(batches);

            System.out.println("Audit Submission Report:");
            for (AuditBatch batch : batches) {
                long totalValue = batch.getTransactions().stream()
                                       .mapToLong(tx -> Math.abs(tx.getAmount()))
                                       .sum();

                System.out.printf(" - Batch ID: %s | Tx Count: %d | Total Value: £%,d%n",
                        batch.getBatchId(), batch.getTransactionCount(), totalValue);
            }
        }
    }
}