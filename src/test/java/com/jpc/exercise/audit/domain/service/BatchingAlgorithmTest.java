package com.jpc.exercise.audit.domain.service;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;

class BatchingAlgorithmTest {

    private final BatchingAlgorithm algorithm = new BatchingAlgorithm();
    private RandomGenerator generator;

    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault(); // Thread-safe, fast
    }

    /**
     * Creates a valid Transaction within domain rules:
     * - Random value between £200 and £500,000
     * - Randomly signed (credit or debit)
     */
    private Transaction generateRandomTransaction() {
        long min = 200L;
        long max = 500_000L;
        long amount = min + (long) (generator.nextDouble() * (max - min));
        boolean isCredit = generator.nextBoolean();
        long signedAmount = isCredit ? amount : -amount;
        return new Transaction(generator, signedAmount);
    }

    @Test
    @DisplayName("Batches must respect £1M max total per batch")
    void shouldRespectBatchSizeLimit() {
        List<Transaction> txs = IntStream.range(0, 1000)
                .mapToObj(i -> generateRandomTransaction())
                .toList();

        List<AuditBatch> batches = algorithm.groupIntoBatches(txs);

        for (AuditBatch batch : batches) {
            long total = batch.getTransactions().stream()
                    .mapToLong(tx -> Math.abs(tx.getAmount()))
                    .sum();

            assertTrue(total <= 1_000_000L, "Batch total exceeds £1,000,000");
            assertFalse(batch.getTransactions().isEmpty(), "Batch must contain transactions");
        }
    }

    @Test
    @DisplayName("Greedy batching packs tightly for known input")
    void shouldPackTightlyUsingGreedySort() {
        List<Transaction> txs = Arrays.asList(
                new Transaction(generator, 500_000L),
                new Transaction(generator, 400_000L),
                new Transaction(generator, 200_000L)
        );

        List<AuditBatch> batches = algorithm.groupIntoBatches(txs);

        assertEquals(2, batches.size(), "Expected 2 batches with greedy strategy");

        long totalTx = batches.stream()
                .mapToInt(AuditBatch::getTransactionCount)
                .sum();

        assertEquals(3, totalTx, "All transactions should be batched");
    }
}
