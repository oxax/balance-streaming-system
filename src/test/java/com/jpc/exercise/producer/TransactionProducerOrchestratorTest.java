package com.jpc.exercise.producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jpc.exercise.account.application.BankAccountService;
import com.jpc.exercise.account.application.BankAccountServiceImpl;
import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.producer.model.ProducerConfig;

class TransactionProducerOrchestratorTest {

    LinkedTransferQueue<Transaction> queue;
    BankAccountService accountService;
    ExecutorService executor;
    private RandomGenerator generator;

    @BeforeEach
    void setup() {
        queue = new LinkedTransferQueue<>();
        accountService = new BankAccountServiceImpl(queue);
        generator = RandomGenerator.getDefault();
        executor = Executors.newFixedThreadPool(2);
    }

    @Test
    void shouldEmitConfiguredTransactionVolume() throws InterruptedException {
        // Define credit and debit producers
        TransactionProducer creditProducer = () -> new Transaction(generator, 100L);
        TransactionProducer debitProducer = () -> new Transaction(generator, -12220L);

        ProducerConfig config = new ProducerConfig(10, 1); // 10 credits + 10 debits over 1 second
        TransactionProducerOrchestrator orchestrator = new TransactionProducerOrchestrator(
                creditProducer,
                debitProducer,
                accountService,
                queue,
                executor);

        orchestrator.startEmitLoops(config);
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertEquals(20, queue.size(), "Expected 10 credits + 10 debits");
        
    }
}