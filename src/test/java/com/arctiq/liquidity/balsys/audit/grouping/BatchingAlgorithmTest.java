package com.arctiq.liquidity.balsys.audit.grouping;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchingAlgorithmTest {

    private BatchingStrategy batchingStrategy;
    private TransactionConfigProperties config;

    @BeforeEach
    void setup() {
        config = AuditTestFixtures.config();
        batchingStrategy = new GreedyBatchingStrategy(Money.of(config.getMaxBatchValue()));
    }

    @Test
    @DisplayName("Batches must respect £1M max total per batch")
    void shouldRespectBatchSizeLimit() {
        List<Transaction> transactions = AuditTestFixtures.randomTransactions(1000, config);
        List<AuditBatch> batches = batchingStrategy.groupIntoBatches(transactions);

        for (AuditBatch batch : batches) {
            double total = batch.getTotalValue().asDouble();
            assertTrue(total <= config.getMaxBatchValue());
            assertFalse(batch.getTransactions().isEmpty());
        }
    }

    @Test
    @DisplayName("Greedy batching packs tightly for known input")
    void shouldPackTightlyUsingGreedySort() {
        List<Transaction> transactions = Arrays.asList(
                AuditTestFixtures.fixedTransaction(500_000.0),
                AuditTestFixtures.fixedTransaction(400_000.0),
                AuditTestFixtures.fixedTransaction(200_000.0));

        List<AuditBatch> batches = batchingStrategy.groupIntoBatches(transactions);

        assertEquals(2, batches.size());
        int totalTx = batches.stream().mapToInt(AuditBatch::getTransactionCount).sum();
        assertEquals(3, totalTx);
    }

    @Test
    @DisplayName("Handles edge case: all transactions near threshold")
    void shouldCreateMultipleBatchesForLargeSameSizeTransactions() {
        List<Transaction> transactions = AuditTestFixtures.thresholdTransactions(3, 999_999.0);
        List<AuditBatch> batches = batchingStrategy.groupIntoBatches(transactions);

        assertEquals(3, batches.size());
        for (AuditBatch batch : batches) {
            assertEquals(1, batch.getTransactionCount());
            assertTrue(batch.getTotalValue().asDouble() <= config.getMaxBatchValue());
        }
    }
}