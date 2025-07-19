package com.arctiq.liquidity.balsys.audit.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingAlgorithm;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AuditFlowIntegrationTest {

    @Test
    void shouldTriggerAuditAfter1000Transactions() {
        LinkedTransferQueue<Transaction> queue = new LinkedTransferQueue<>();
        SpyAuditNotifier spyNotifier = new SpyAuditNotifier();
        AuditBatchPersistence persistence = new InMemoryAuditBatchStore();

        BankAccountService accountService = new BankAccountServiceImpl(queue);
        AuditProcessingService auditService = new AuditProcessingService(
                1000,
                1000,
                1000,
                1,
                new BatchingAlgorithm(),
                spyNotifier,
                persistence,
                new MetricsCollector(new SimpleMeterRegistry()));

        for (int i = 0; i < 1000; i++) {
            double amount = 250_000.0;
            Transaction tx = new Transaction(TransactionId.generate(), Money.of(amount));
            accountService.processTransaction(tx);
        }

        auditService.flushIfThresholdMet();

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
