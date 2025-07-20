package com.arctiq.liquidity.balsys.producer.orchestration;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionInvalid;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;

public class TransactionProducerOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducerOrchestrator.class);

    private final TransactionProducer creditProducer;
    private final TransactionProducer debitProducer;
    private final BankAccountService accountService;

    private final AuditProcessingService auditProcessingService;

    private final LinkedTransferQueue<Transaction> queue;
    private final ExecutorService executor;
    private final MetricsCollector metricsCollector;

    private final Counter producedCounter;
    private final Counter submittedCounter;
    private final AtomicInteger currentEmissionRate = new AtomicInteger(0);

    public TransactionProducerOrchestrator(
            TransactionProducer creditProducer,
            TransactionProducer debitProducer,
            BankAccountService accountService,
            AuditProcessingService auditProcessingService,
            LinkedTransferQueue<Transaction> queue,
            ExecutorService executor,
            MeterRegistry meterRegistry,
            MetricsCollector metricsCollector) {

        this.creditProducer = creditProducer;
        this.debitProducer = debitProducer;
        this.accountService = accountService;
        this.auditProcessingService = auditProcessingService;
        this.queue = queue;
        this.executor = executor;
        this.metricsCollector = metricsCollector;

        this.producedCounter = Counter.builder("transactions.produced.total")
                .description("Total transactions produced")
                .register(meterRegistry);

        this.submittedCounter = Counter.builder("transactions.submitted.total")
                .description("Total transactions submitted to BankAccountService")
                .register(meterRegistry);

        meterRegistry.gauge("transactions.emission.rate", currentEmissionRate);
    }

    public void startEmitLoops(ProducerConfig producerConfig) {
        logger.debug("Starting emission: {} tx/stream every {}s", producerConfig.count(),
                producerConfig.intervalSeconds());

        executor.submit(() -> emitLoop(creditProducer, producerConfig));
        executor.submit(() -> emitLoop(debitProducer, producerConfig));
    }

    private void emitLoop(TransactionProducer producer, ProducerConfig producerConfig) {
        long spacingNanos = TimeUnit.SECONDS.toNanos(producerConfig.intervalSeconds()) / producerConfig.count();
        long nextTick = System.nanoTime();
        logger.debug("ProducerConfig state: count={}, intervalSeconds={}",
                producerConfig.count(), producerConfig.intervalSeconds());

        int successfulEmissions = 0;

        for (int i = 0; i < producerConfig.count(); i++) {
            try {
                Transaction tx = producer.produce();
                producedCounter.increment();
                currentEmissionRate.incrementAndGet();

                accountService.processTransaction(tx);
                submittedCounter.increment();
                metricsCollector.recordTransaction(tx);

                successfulEmissions++;

            } catch (Exception ex) {
                logger.warn("Transaction emission failed on iteration {}: {}", i, ex.getMessage(), ex);
                metricsCollector.recordTransactionOutcome(new TransactionInvalid(null, ex.getMessage()));
            }

            nextTick += spacingNanos;
            long sleepTime = nextTick - System.nanoTime();
            if (sleepTime > 0) {
                LockSupport.parkNanos(sleepTime);
            }
        }

        logger.debug("Emission completed for producer: {}. Successful emissions: {}",
                producer.getClass().getSimpleName(), successfulEmissions);

        currentEmissionRate.set(0);
    }

    public void triggerAuditIfThresholdMet() {
        auditProcessingService.flushIfThresholdMet();
    }
}