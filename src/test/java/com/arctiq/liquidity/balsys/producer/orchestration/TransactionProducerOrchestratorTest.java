package com.arctiq.liquidity.balsys.producer.orchestration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arctiq.liquidity.balsys.account.application.BankAccountService;
import com.arctiq.liquidity.balsys.account.application.BankAccountServiceImpl;
import com.arctiq.liquidity.balsys.audit.application.AuditStatsService;
import com.arctiq.liquidity.balsys.audit.dispatch.ConsoleAuditNotifier;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.grouping.FirstFitDecreasingBatchingStrategy;
import com.arctiq.liquidity.balsys.audit.ingestion.AuditProcessingService;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.audit.persistence.InMemoryAuditBatchStore;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.producer.channel.TransactionProducer;
import com.arctiq.liquidity.balsys.producer.config.ProducerConfig;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.testfixtures.AuditTestFixtures;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class TransactionProducerOrchestratorTest {

        private static final Logger logger = LoggerFactory.getLogger(TransactionProducerOrchestratorTest.class);

        LinkedTransferQueue<Transaction> queue;
        BankAccountService accountService;
        AuditProcessingService auditProcessingService;
        ExecutorService executor;
        private MetricsCollector metrics;
        private TransactionConfigProperties configProperties;

        @BeforeEach
        void setup() {
                configProperties = AuditTestFixtures.config();
                queue = new LinkedTransferQueue<>();
                metrics = new MetricsCollector(new SimpleMeterRegistry());
                AuditStatsService statsService = new AuditStatsService();
                executor = Executors.newFixedThreadPool(2);
                AuditBatchPersistence persistence = new InMemoryAuditBatchStore();
                AuditNotifier notifier = new ConsoleAuditNotifier();
                BatchingStrategy batchingStrategy = new FirstFitDecreasingBatchingStrategy(
                                Money.of(configProperties.getMaxBatchValue()));
                auditProcessingService = new AuditProcessingService(queue, configProperties, batchingStrategy, notifier,
                                persistence, metrics);
                accountService = new BankAccountServiceImpl(queue, configProperties, metrics);
        }

        @Test
        void shouldEmitConfiguredTransactionVolume() throws InterruptedException {
                TransactionProducer creditProducer = () -> AuditTestFixtures.randomCredit(configProperties);
                TransactionProducer debitProducer = () -> AuditTestFixtures.randomDebit(configProperties);

                ProducerConfig producerConfig = new ProducerConfig(10, 1); // 10 credits + 10 debits over 1 second
                logger.info("ProducerConfig state: count={}, intervalSeconds={}",
                                producerConfig.count(), producerConfig.intervalSeconds());

                TransactionProducerOrchestrator orchestrator = new TransactionProducerOrchestrator(creditProducer,
                                debitProducer, accountService, auditProcessingService, new SimpleMeterRegistry(),
                                executor, metrics);

                logger.info("ProducerConfig state after orchestrator init: count={}, intervalSeconds={}",
                                producerConfig.count(), producerConfig.intervalSeconds());

                orchestrator.startEmitLoops(producerConfig);
                executor.shutdown();
                executor.awaitTermination(2, TimeUnit.SECONDS);

                assertEquals(20, queue.size(), "Expected 10 credits + 10 debits");
        }
}