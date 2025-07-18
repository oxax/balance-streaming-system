package com.jpc.exercise.producer;

import com.jpc.exercise.account.application.BankAccountService;
import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.producer.model.ProducerConfig;
import com.jpc.exercise.producer.TransactionProducer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionProducerOrchestrator {

    private static final Logger logger = Logger.getLogger(TransactionProducerOrchestrator.class.getName());

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
        executor.submit(() -> emitLoop(creditProducer, config));
        executor.submit(() -> emitLoop(debitProducer, config));
    }

    private void emitLoop(TransactionProducer producer, ProducerConfig config) {
        long spacingNanos = TimeUnit.SECONDS.toNanos(config.intervalSeconds()) / config.count();
        long nextTick = System.nanoTime();

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
    }
}