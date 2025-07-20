package com.arctiq.liquidity.balsys.producer.simulation;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.producer.orchestration.TransactionProducerOrchestrator;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import java.util.concurrent.*;

import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;

@Component
public class TransactionSimulationManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionSimulationManager.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService emitExecutor = Executors.newFixedThreadPool(2);
    private final TransactionProducerOrchestrator orchestrator;
    private final MetricsCollector metricsCollector;

    public TransactionSimulationManager(
            TransactionConfigProperties config,
            MetricsCollector metricsCollector,
            MeterRegistry meterRegistry,
            LinkedTransferQueue<Transaction> txQueue,
            BankAccountService accountService,
            AuditProcessingService auditProcessingService) {

        this.metricsCollector = metricsCollector;

        TransactionProducer creditProducer = () -> AuditTestFixtures.randomCredit(config);
        TransactionProducer debitProducer = () -> AuditTestFixtures.randomDebit(config);

        this.orchestrator = new TransactionProducerOrchestrator(
                creditProducer,
                debitProducer,
                accountService,
                auditProcessingService,
                txQueue,
                emitExecutor,
                meterRegistry,
                metricsCollector);
    }

    public void startSimulation(int transactionCount, int durationSeconds) {
        logger.info("🟢 Starting simulation with {} transactions over {} seconds", transactionCount, durationSeconds);
        ProducerConfig config = new ProducerConfig(transactionCount, durationSeconds);
        orchestrator.startEmitLoops(config);
        orchestrator.triggerAuditIfThresholdMet();
        scheduler.scheduleAtFixedRate(metricsCollector::logRuntimeMetrics, 5, 10, TimeUnit.SECONDS);
    }

    public void stopSimulation() {
        logger.info("🛑 Stopping simulation...");
        scheduler.shutdownNow();
        emitExecutor.shutdownNow();
        metricsCollector.logRuntimeMetrics(); // final snapshot
    }

    @PreDestroy
    public void cleanup() {
        stopSimulation();
    }
}