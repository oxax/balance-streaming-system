package com.arctiq.liquidity.balsys.audit.ingestion;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.FirstFitDecreasingBatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.GreedyBatchingStrategy;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

import static org.junit.jupiter.api.Assertions.*;

import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

class AuditFlowIntegrationTest {

    @Test
    void shouldTriggerAuditAfter1000Transactions() throws InterruptedException {
        AuditStatsService statsService = new AuditStatsService();
        TransactionConfigProperties config = AuditTestFixtures.config();
        MetricsCollector metrics = new MetricsCollector(new SimpleMeterRegistry());
        AuditBatchPersistence persistence = new InMemoryAuditBatchStore();
        AuditNotifier notifier = new ConsoleAuditNotifier();
        // BatchingStrategy batchingStrategy = new
        // GreedyBatchingStrategy(Money.of(config.getMaxBatchValue()));
        BatchingStrategy batchingStrategy = new FirstFitDecreasingBatchingStrategy(Money.of(config.getMaxBatchValue()));
        LinkedTransferQueue<Transaction> queue = new LinkedTransferQueue<>();

        BankAccountService accountService = new BankAccountServiceImpl(queue, config, metrics);

        AuditProcessingService auditService = new AuditProcessingService(
                queue,
                config,
                batchingStrategy,
                notifier,
                persistence,
                metrics);

        persistence.clear();
        List<Transaction> txs = AuditTestFixtures.thresholdTransactions(config.getSubmissionLimit(), 250_000.0);
        txs.forEach(accountService::processTransaction);

        auditService.flushIfThresholdMet();

        int maxWaitMillis = 1000;
        int elapsed = 0;
        while (persistence.fetchAll().isEmpty() && elapsed < maxWaitMillis) {
            Thread.sleep(50);
            elapsed += 50;
        }

        List<AuditBatch> persisted = persistence.fetchAll();
        assertFalse(persisted.isEmpty(), "Audit should persist batches after threshold");
        int totalTx = persisted.stream().mapToInt(AuditBatch::getTransactionCount).sum();
        assertEquals(config.getSubmissionLimit(), totalTx, "Expected all submitted transactions to be batched");
    }
}