package com.arctiq.liquidity.balsys.producer.simulation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.producer.orchestration.TransactionProducerOrchestrator;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;

public class TransactionSimulationManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionSimulationManager.class);

    private ScheduledExecutorService scheduler;
    private final ExecutorService emitExecutor = Executors.newFixedThreadPool(2);
    private final TransactionProducerOrchestrator orchestrator;

    public TransactionSimulationManager(
            TransactionConfigProperties config,
            MetricsCollector metricsCollector,
            MeterRegistry meterRegistry,
            LinkedTransferQueue<Transaction> txQueue,
            BankAccountService accountService,
            AuditProcessingService auditProcessingService) {

        TransactionProducer creditProducer = () -> AuditTestFixtures.randomCredit(config);
        TransactionProducer debitProducer = () -> AuditTestFixtures.randomDebit(config);

        this.orchestrator = new TransactionProducerOrchestrator(
                creditProducer,
                debitProducer,
                accountService,
                auditProcessingService,
                meterRegistry,
                emitExecutor,
                metricsCollector);
    }

    public synchronized void startSimulation(int transactionCount, int durationSeconds) {
        logger.info("Starting recurring simulation: emit {} every {} seconds", transactionCount, durationSeconds);

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            logger.info("Scheduler reinitialized");
        }

        ProducerConfig config = new ProducerConfig(transactionCount, durationSeconds);

        scheduler.scheduleAtFixedRate(() -> emitExecutor.submit(() -> {
            logger.debug("Scheduled emit triggered");
            orchestrator.startEmitLoops(config);
        }), 0, durationSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stopSimulation() {
        logger.info("Stopping simulation...");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            logger.info("Scheduler shutdown complete");
        }

        emitExecutor.shutdownNow();
    }

    @PreDestroy
    public void cleanup() {
        stopSimulation();
    }
}