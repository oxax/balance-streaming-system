package com.arctiq.liquidity.balsys.producer.orchestration;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.outcome.TransactionInvalid;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class TransactionProducerOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducerOrchestrator.class);

    private final TransactionProducer creditProducer;
    private final TransactionProducer debitProducer;
    private final BankAccountService accountService;
    private final AuditProcessingService auditProcessingService;
    private final ExecutorService executor;
    private final MetricsCollector metricsCollector;

    private final Counter producedCounter;
    private final Counter submittedCounter;
    private final AtomicInteger currentEmissionRate = new AtomicInteger();

    public TransactionProducerOrchestrator(
            TransactionProducer creditProducer,
            TransactionProducer debitProducer,
            BankAccountService accountService,
            AuditProcessingService auditProcessingService,
            MeterRegistry meterRegistry,
            ExecutorService executor,
            MetricsCollector metricsCollector) {

        this.creditProducer = creditProducer;
        this.debitProducer = debitProducer;
        this.accountService = accountService;
        this.auditProcessingService = auditProcessingService;
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

    public void startEmitLoops(ProducerConfig config) {
        logger.info("Starting emission: {} tx/stream over {}s", config.count(), config.intervalSeconds());
        executor.submit(() -> emitLoop(creditProducer, config));
        executor.submit(() -> emitLoop(debitProducer, config));
    }

    private void emitLoop(TransactionProducer producer, ProducerConfig config) {
        long spacingNanos = TimeUnit.SECONDS.toNanos(config.intervalSeconds()) / config.count();
        long nextTick = System.nanoTime();
        int successfulEmissions = 0;

        int flushRatio = Math.max(2, config.count() / 5);
        Set<Integer> flushPoints = IntStream.rangeClosed(1, config.count())
                .filter(it -> it % flushRatio == 0 || it == config.count())
                .boxed()
                .collect(Collectors.toSet());

        logger.debug("Planned audit flush points: {}", flushPoints);

        for (int i = 1; i <= config.count(); i++) {
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

            logger.debug("flushPoints.contains(i): {} {}", i, flushPoints.contains(i));
            if (flushPoints.contains(i)) {
                auditProcessingService.flushIfThresholdMet();
            }

            nextTick += spacingNanos;
            long sleepTime = nextTick - System.nanoTime();
            if (sleepTime > 0) {
                LockSupport.parkNanos(sleepTime);
            }
        }

        logger.debug("Emission completed [{}] | Successful tx: {}",
                producer.getClass().getSimpleName(),
                successfulEmissions);

        currentEmissionRate.set(0);
    }
}