package com.jpc.exercise.audit.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.account.domain.model.Transaction;

class InMemoryAuditBatchStoreTest {

    private AuditBatchPersistence store;
    private final RandomGenerator generator = RandomGenerator.getDefault();

    @BeforeEach
    void setup() {
        store = new InMemoryAuditBatchStore();
    }

    @Test
    void shouldSaveAndLoadBatchById() {
        String batchId = "batch-001";
        List<Transaction> txs = List.of(
                new Transaction(generator, 300_000L),
                new Transaction(generator, -150_000L)
        );

        store.save(batchId, txs);

        Optional<List<Transaction>> retrieved = store.load(batchId);
        assertTrue(retrieved.isPresent(), "Expected batch to be retrievable");
        assertEquals(2, retrieved.get().size(), "Expected 2 transactions in batch");
    }

    @Test
    void shouldTrackPendingBatchIds() {
        String batchId = "pending-batch";
        List<Transaction> txs = List.of(new Transaction(generator, 200_000L));

        store.save(batchId, txs);

        List<String> pending = store.findPendingBatchIds();
        assertTrue(pending.contains(batchId), "Batch should be marked as pending");
    }

    @Test
    void shouldMarkBatchAsSubmitted() {
        String batchId = "submitted-batch";
        List<Transaction> txs = List.of(new Transaction(generator, 450_000L));

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