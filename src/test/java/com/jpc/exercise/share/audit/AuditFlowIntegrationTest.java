package com.jpc.exercise.share.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.account.application.BankAccountService;
import com.jpc.exercise.account.application.BankAccountServiceImpl;
import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.application.AuditProcessingService;
import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.audit.domain.service.BatchingAlgorithm;
import com.jpc.exercise.shared.audit.AuditNotifier;

class AuditFlowIntegrationTest {

    private RandomGenerator generator;
    @BeforeEach
    void setup() {
        generator = RandomGenerator.getDefault();
    }

    @Test
    void shouldTriggerAuditAfter1000Transactions() {
        LinkedTransferQueue<Transaction> queue = new LinkedTransferQueue<>();

        // Spy notifier to inspect submission
        SpyAuditNotifier spyNotifier = new SpyAuditNotifier();

        BankAccountService accountService = new BankAccountServiceImpl(queue);
        AuditProcessingService auditService = new AuditProcessingService(queue, new BatchingAlgorithm(), spyNotifier);

        // Simulate pushing 1000 transactions
        for (int i = 0; i < 1000; i++) {
            accountService.processTransaction(new Transaction(generator, 250_000L));
        }

        // Trigger batch processing
        auditService.runAuditCycle();

        // Verify batches were submitted
        assertFalse(spyNotifier.submittedBatches.isEmpty(), "Audit submission should have occurred");
        int totalTx = spyNotifier.submittedBatches.stream()
                          .mapToInt(batch -> batch.getTransactions().size())
                          .sum();
        assertEquals(1000, totalTx, "Total transactions batched should be 1000");
    }

    static class SpyAuditNotifier implements AuditNotifier {
        List<AuditBatch> submittedBatches = new ArrayList<>();

        @Override
        public void submit(List<AuditBatch> batches) {
            submittedBatches.addAll(batches);
        }
    }
}