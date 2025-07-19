package com.arctiq.liquidity.balsys.producer.orchestration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

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
        TransactionProducer creditProducer = () -> {
            double amount = 100.0;
            return new Transaction(TransactionId.generate(), Money.of(amount));
        };

        TransactionProducer debitProducer = () -> {
            double amount = -12_220.0;
            return new Transaction(TransactionId.generate(), Money.of(amount));
        };

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
