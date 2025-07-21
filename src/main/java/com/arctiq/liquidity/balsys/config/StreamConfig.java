package com.arctiq.liquidity.balsys.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.FirstFitDecreasingBatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.GreedyBatchingStrategy;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.producer.channel.CreditProducer;
import com.arctiq.liquidity.balsys.producer.channel.DebitProducer;
import com.arctiq.liquidity.balsys.producer.orchestration.TransactionProducerOrchestrator;
import com.arctiq.liquidity.balsys.producer.simulation.TransactionSimulationManager;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class StreamConfig {

    // 🧵 Threading
    @Bean
    public ExecutorService auditExecutor() {
        return Executors.newFixedThreadPool(2);
    }

    // 💰 Producers
    @Bean
    public CreditProducer creditProducer() {
        return new CreditProducer(new TransactionConfigProperties());
    }

    @Bean
    public DebitProducer debitProducer() {
        return new DebitProducer(new TransactionConfigProperties());
    }

    // 🛠️ Orchestration
    @Bean
    public TransactionProducerOrchestrator producerOrchestrator(
            CreditProducer creditProducer,
            DebitProducer debitProducer,
            BankAccountService accountService,
            AuditProcessingService auditProcessingService,
            LinkedTransferQueue<Transaction> transactionQueue,
            ExecutorService auditExecutor,
            MeterRegistry meterRegistry,
            MetricsCollector metricsCollector) {

        return new TransactionProducerOrchestrator(
                creditProducer,
                debitProducer,
                accountService,
                auditProcessingService,
                meterRegistry,
                auditExecutor,
                metricsCollector);
    }

    // 📦 Batching strategies
    @Bean
    @ConditionalOnProperty(name = "transaction.batching.strategy", havingValue = "greedy")
    public BatchingStrategy greedyBatchingStrategy(BatchingConfigProperties config) {
        return new GreedyBatchingStrategy(Money.of(config.getValueLimit()));
    }

    @Bean
    @ConditionalOnProperty(name = "transaction.batching.strategy", havingValue = "ffd")
    public BatchingStrategy firstFitDecreasingBatchingStrategy(BatchingConfigProperties config) {
        return new FirstFitDecreasingBatchingStrategy(Money.of(config.getValueLimit()));
    }

    // 🎛️ Simulation manager
    @Bean
    public TransactionSimulationManager simulationManager(
            TransactionConfigProperties config,
            MetricsCollector metricsCollector,
            MeterRegistry meterRegistry,
            LinkedTransferQueue<Transaction> transactionQueue,
            BankAccountService accountService,
            AuditProcessingService auditProcessingService) {

        return new TransactionSimulationManager(
                config,
                metricsCollector,
                meterRegistry,
                transactionQueue,
                accountService,
                auditProcessingService);
    }
}
