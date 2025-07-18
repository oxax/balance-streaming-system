package com.jpc.exercise.producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jpc.exercise.account.application.BankAccountService;
import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.producer.model.ProducerConfig;

public class TransactionProducerOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducerOrchestrator.class);

    private final TransactionProducer creditProducer;
    private final TransactionProducer debitProducer;
    private final BankAccountService accountService;
    private final LinkedTransferQueue<Transaction> queue;
    private final ExecutorService executor;

    public TransactionProducerOrchestrator(
            TransactionProducer creditProducer,
            TransactionProducer debitProducer,
            BankAccountService accountService,
            LinkedTransferQueue<Transaction> queue,
            ExecutorService executor) {
        this.creditProducer = creditProducer;
        this.debitProducer = debitProducer;
        this.accountService = accountService;
        this.queue = queue;
        this.executor = executor;
    }

    public void startEmitLoops(ProducerConfig config) {
        logger.info("Starting transaction emission: {} transactions per stream over {} seconds.",
                config.count(), config.intervalSeconds());
        executor.submit(() -> emitLoop(creditProducer, config));
        executor.submit(() -> emitLoop(debitProducer, config));
    }

    private void emitLoop(TransactionProducer producer, ProducerConfig config) {
        long spacingNanos = TimeUnit.SECONDS.toNanos(config.intervalSeconds()) / config.count();
        long nextTick = System.nanoTime();

        try {
            for (int i = 0; i < config.count(); i++) {
                Transaction tx = producer.produce();
                accountService.processTransaction(tx);
                queue.offer(tx);

                nextTick += spacingNanos;
                long sleepTime = nextTick - System.nanoTime();
                if (sleepTime > 0) {
                    LockSupport.parkNanos(sleepTime);
                }
            }
            logger.info("Emission completed for producer: {}", producer.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Transaction emission failed for producer {}: {}", producer.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}