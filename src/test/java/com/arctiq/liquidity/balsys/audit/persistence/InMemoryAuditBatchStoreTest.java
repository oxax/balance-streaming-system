package com.arctiq.liquidity.balsys.audit.persistence;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.TransactionId;

class InMemoryAuditBatchStoreTest {

    private AuditBatchPersistence store;

    @BeforeEach
    void setup() {
        store = new InMemoryAuditBatchStore();
    }

    @Test
    void shouldSaveAndLoadBatchById() {
        String batchId = "batch-001";
        List<Transaction> txs = List.of(
                new Transaction(TransactionId.generate(), Money.of(300_000.0)),
                new Transaction(TransactionId.generate(), Money.of(-150_000.0)));

        store.save(batchId, txs);

        Optional<List<Transaction>> retrieved = store.load(batchId);
        assertTrue(retrieved.isPresent(), "Expected batch to be retrievable");
        assertEquals(2, retrieved.get().size(), "Expected 2 transactions in batch");
    }

    @Test
    void shouldTrackPendingBatchIds() {
        String batchId = "pending-batch";
        List<Transaction> txs = List.of(
                new Transaction(TransactionId.generate(), Money.of(200_000.0)));

        store.save(batchId, txs);

        List<String> pending = store.findPendingBatchIds();
        assertTrue(pending.contains(batchId), "Batch should be marked as pending");
    }

    @Test
    void shouldMarkBatchAsSubmitted() {
        String batchId = "submitted-batch";
        List<Transaction> txs = List.of(
                new Transaction(TransactionId.generate(), Money.of(450_000.0)));

        store.save(batchId, txs);
        store.markSubmitted(batchId);

        List<String> pending = store.findPendingBatchIds();
        assertFalse(pending.contains(batchId), "Batch should no longer be pending");
    }

    @Test
    void shouldHandleUnknownBatchGracefully() {
        Optional<List<Transaction>> result = store.load("nonexistent");
        assertTrue(result.isEmpty(), "Should not retrieve nonexistent batch");
    }
}
