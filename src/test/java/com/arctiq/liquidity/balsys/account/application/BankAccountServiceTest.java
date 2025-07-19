package com.arctiq.liquidity.balsys.account.application;

import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.TransactionId;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountServiceTest {

    private BankAccountService service;
    private LinkedTransferQueue<Transaction> queue;
    private TransactionConfigProperties config;
    private MetricsCollector metrics;

    @BeforeEach
    void setup() {
        config = AuditTestFixtures.config();
        queue = new LinkedTransferQueue<>();
        metrics = new MetricsCollector(new SimpleMeterRegistry());
        service = new BankAccountServiceImpl(queue, config, metrics);
    }

    @Test
    @DisplayName("Processes single credit correctly")
    void shouldProcessSingleCredit() {
        Transaction tx = AuditTestFixtures.fixedTransaction(300_000.0);
        service.processTransaction(tx);

        assertEquals(300_000.0, service.retrieveBalance());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("Processes single debit correctly")
    void shouldProcessSingleDebit() {
        Transaction tx = AuditTestFixtures.fixedTransaction(-200_000.0);
        service.processTransaction(tx);

        assertEquals(-200_000.0, service.retrieveBalance());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("Accumulates multiple transactions accurately")
    void shouldAccumulateMultipleTransactions() {
        service.processTransaction(AuditTestFixtures.fixedTransaction(400_000.0));
        service.processTransaction(AuditTestFixtures.fixedTransaction(-250_000.0));
        service.processTransaction(AuditTestFixtures.fixedTransaction(100_000.0));

        double expectedBalance = 400_000.0 - 250_000.0 + 100_000.0;
        assertEquals(expectedBalance, service.retrieveBalance());
        assertEquals(3, queue.size());
    }

    @Test
    @DisplayName("Handles concurrent transactions safely")
    void shouldHandleConcurrentTransactions() throws InterruptedException {
        int threads = 100;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                Transaction tx = AuditTestFixtures.fixedTransaction(10_000.0);
                service.processTransaction(tx);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threads * 10_000.0, service.retrieveBalance());
        assertEquals(threads, queue.size());
    }

    @Test
    @DisplayName("Rejects null transaction input")
    void shouldRejectNullTransaction() {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> service.processTransaction(null));

        assertEquals("Transaction must not be null", thrown.getMessage());
    }

    @Test
    @DisplayName("Rejects transaction outside valid range")
    void shouldRejectInvalidAmount() {
        Transaction tx = new Transaction(TransactionId.generate(), Money.of(199.0));
        TransactionValidationException thrown = assertThrows(
                TransactionValidationException.class,
                () -> service.processTransaction(tx));

        assertTrue(thrown.getMessage().contains("out of range"));
    }
}