package com.arctiq.liquidity.balsys.account.application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.exception.TransactionValidationException;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

class BankAccountServiceTest {

    private BankAccountService service;
    private LinkedTransferQueue<Transaction> queue;

    @BeforeEach
    void setup() {
        queue = new LinkedTransferQueue<>();
        service = new BankAccountServiceImpl(queue);
    }

    @Test
    @DisplayName("Processes single credit correctly")
    void shouldProcessSingleCredit() {
        Transaction tx = new Transaction(TransactionId.generate(), Money.of(300_000.0));
        service.processTransaction(tx);

        assertEquals(300_000.0, service.retrieveBalance());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("Processes single debit correctly")
    void shouldProcessSingleDebit() {
        Transaction tx = new Transaction(TransactionId.generate(), Money.of(-200_000.0));
        service.processTransaction(tx);

        assertEquals(-200_000.0, service.retrieveBalance());
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("Accumulates multiple transactions accurately")
    void shouldAccumulateMultipleTransactions() {
        service.processTransaction(new Transaction(TransactionId.generate(), Money.of(400_000.0)));
        service.processTransaction(new Transaction(TransactionId.generate(), Money.of(-250_000.0)));
        service.processTransaction(new Transaction(TransactionId.generate(), Money.of(100_000.0)));

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
                Transaction tx = new Transaction(TransactionId.generate(), Money.of(10_000.0));
                service.processTransaction(tx);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threads * 10_000.0, service.retrieveBalance());
        assertEquals(100, queue.size());
    }

    @Test
    @DisplayName("Rejects null transaction input")
    void shouldRejectNullTransaction() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.processTransaction(null));
        assertEquals("Transaction must not be null", thrown.getMessage());
    }

    @Test
    @DisplayName("Rejects transaction outside valid range")
    void shouldRejectInvalidAmount() {
        Money invalidAmount = Money.of(199.0);
        Transaction tx = new Transaction(TransactionId.generate(), invalidAmount);

        TransactionValidationException thrown = assertThrows(
                TransactionValidationException.class,
                () -> service.processTransaction(tx));

        assertTrue(thrown.getMessage().contains("out of range"));
    }
}
