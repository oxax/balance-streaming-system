package com.jpc.exercise.share.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.audit.infrastructure.notifier.ConsoleAuditNotifier;
import com.jpc.exercise.shared.audit.AuditNotifier;

class AuditNotifierStubTest {

    private RandomGenerator generator;
    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault();
    }

    @Test
    void shouldLogBatchesToConsole() {
        AuditNotifier stub = new ConsoleAuditNotifier();
        List<AuditBatch> mockBatches = new ArrayList<>();

        // Add dummy batches with mock transactions
        mockBatches.add(new AuditBatch("batch-1", List.of(
            new Transaction(generator, -100_000L),
            new Transaction(generator, 50_000L))));

        stub.submit(mockBatches);

        // No assertion — manually verify console structure/log
    }
}
