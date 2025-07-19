package com.arctiq.liquidity.balsys.producer.lifecycle;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.producer.config.ProducerSettings;
import com.arctiq.liquidity.balsys.producer.orchestration.TransactionProducerOrchestrator;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class TransactionSimulationManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionSimulationManager.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService emitExecutor = Executors.newFixedThreadPool(2);

    private final TransactionProducer creditProducer;
    private final TransactionProducer debitProducer;
    private final TransactionConfigProperties config;
    private final TransactionProducerOrchestrator orchestrator;
    private final MetricsCollector metricsCollector;
    private final ProducerSettings settings;

    public TransactionSimulationManager(
            ProducerSettings settings,
            TransactionProducer creditProducer,
            TransactionProducer debitProducer,
            TransactionConfigProperties config,
            MetricsCollector metricsCollector,
            MeterRegistry meterRegistry,
            LinkedTransferQueue<Transaction> txQueue,
            BankAccountService accountService) {

        this.settings = settings;
        this.creditProducer = creditProducer;
        this.debitProducer = debitProducer;
        this.config = config;
        this.metricsCollector = metricsCollector;

        this.orchestrator = new TransactionProducerOrchestrator(creditProducer, debitProducer, accountService, txQueue,
                emitExecutor, meterRegistry, metricsCollector);
    }

    @PostConstruct
    public void start() {
        logger.info("🏁 Transaction simulation manager starting...");
        ProducerConfig producerConfig = settings.toConfig();
        orchestrator.startEmitLoops(producerConfig);
        scheduler.scheduleAtFixedRate(metricsCollector::logRuntimeMetrics, 5, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        logger.info("🛑 Transaction simulation manager stopping...");
        scheduler.shutdown();
        emitExecutor.shutdown();
        metricsCollector.logRuntimeMetrics(); // final snapshot
    }
}