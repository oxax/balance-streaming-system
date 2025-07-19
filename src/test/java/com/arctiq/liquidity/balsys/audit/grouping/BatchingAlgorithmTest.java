package com.arctiq.liquidity.balsys.audit.grouping;

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

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

class BatchingAlgorithmTest {

    private BatchingAlgorithm algorithm;
    private RandomGenerator generator;

    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault();
        algorithm = new BatchingAlgorithm(new TransactionConfigProperties());
    }

    private Transaction generateRandomTransaction() {
        double min = 200.0;
        double max = 500_000.0;
        double amount = min + generator.nextDouble() * (max - min);
        boolean isCredit = generator.nextBoolean();
        double signedAmount = isCredit ? amount : -amount;
        return new Transaction(TransactionId.generate(), Money.of(signedAmount));
    }

    @Test
    @DisplayName("Batches must respect £1M max total per batch")
    void shouldRespectBatchSizeLimit() {
        List<Transaction> transactions = IntStream.range(0, 1000)
                .mapToObj(i -> generateRandomTransaction())
                .toList();

        List<AuditBatch> batches = algorithm.groupIntoBatches(transactions);

        for (AuditBatch batch : batches) {
            double total = batch.getTransactions().stream()
                    .mapToDouble(tx -> tx.absoluteValue().asDouble())
                    .sum();

            assertTrue(total <= 1_000_000.0, "Batch total exceeds £1,000,000");
            assertFalse(batch.getTransactions().isEmpty(), "Batch must contain transactions");
        }
    }

    @Test
    @DisplayName("Greedy batching packs tightly for known input")
    void shouldPackTightlyUsingGreedySort() {
        List<Transaction> transactions = Arrays.asList(
                new Transaction(TransactionId.generate(), Money.of(500_000.0)),
                new Transaction(TransactionId.generate(), Money.of(400_000.0)),
                new Transaction(TransactionId.generate(), Money.of(200_000.0)) // causes new batch
        );

        List<AuditBatch> batches = algorithm.groupIntoBatches(transactions);

        assertEquals(2, batches.size(), "Expected 2 batches with greedy strategy");

        int totalTx = batches.stream()
                .mapToInt(AuditBatch::getTransactionCount)
                .sum();

        assertEquals(3, totalTx, "All transactions should be batched");
    }

    @Test
    @DisplayName("Handles edge case: all transactions near threshold")
    void shouldCreateMultipleBatchesForLargeSameSizeTransactions() {
        List<Transaction> transactions = Arrays.asList(
                new Transaction(TransactionId.generate(), Money.of(999_999.0)),
                new Transaction(TransactionId.generate(), Money.of(999_999.0)),
                new Transaction(TransactionId.generate(), Money.of(999_999.0)));

        List<AuditBatch> batches = algorithm.groupIntoBatches(transactions);

        assertEquals(3, batches.size(), "Each large transaction should force a separate batch");

        for (AuditBatch batch : batches) {
            assertEquals(1, batch.getTransactionCount());
            assertTrue(batch.getTotalValue().asDouble() <= 1_000_000.0);
        }
    }
}
