package com.arctiq.liquidity.balsys.audit.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.Timer;

public class AuditProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AuditProcessingService.class);

    private final BlockingQueue<Transaction> transactionQueue;
    private final BatchingStrategy batchingStrategy;
    private final AuditNotifier notifier;
    private final AuditBatchPersistence auditBatchPersistence;
    private final MetricsCollector metrics;
    private final ExecutorService auditExecutor;
    private final int submissionLimit;
    private final AtomicBoolean submitting = new AtomicBoolean(false);

    public AuditProcessingService(
            BlockingQueue<Transaction> transactionQueue,
            TransactionConfigProperties config,
            BatchingStrategy batchingStrategy,
            AuditNotifier notifier,
            AuditBatchPersistence auditBatchPersistence,
            MetricsCollector metrics) {

        this.transactionQueue = transactionQueue;
        this.submissionLimit = config.getSubmissionLimit();
        this.batchingStrategy = batchingStrategy;
        this.notifier = notifier;
        this.auditBatchPersistence = auditBatchPersistence;
        this.metrics = metrics;
        this.auditExecutor = Executors.newFixedThreadPool(config.getAuditThreads());
    }

    public void flushIfThresholdMet() {
        logger.debug(" transactionQueue.size() {} submissionLimit {} submitting {} ", transactionQueue.size(),
                submissionLimit, submitting);
        boolean thresholdMet = transactionQueue.size() >= submissionLimit && submitting.compareAndSet(false, true);
        logger.debug("thresholdMet {}", thresholdMet);
        if (thresholdMet) {
            auditExecutor.submit(this::runAuditCycle);
        }
    }

    private void runAuditCycle() {
        Timer.Sample latencySample = metrics.startAuditLatencySample();

        try {
            List<Transaction> drained = new ArrayList<>();
            transactionQueue.drainTo(drained, submissionLimit);
            metrics.updateQueueSize(transactionQueue.size());

            if (drained.isEmpty())
                return;

            String batchId = UUID.randomUUID().toString();
            auditBatchPersistence.save(batchId, drained);

            List<AuditBatch> grouped = batchingStrategy.groupIntoBatches(drained);
            notifier.submit(grouped);
            auditBatchPersistence.markSubmitted(batchId);
            metrics.recordAuditSubmission(grouped);

        } catch (Exception e) {
            logger.error("Audit cycle failure: {}", e.getMessage(), e);
        } finally {
            submitting.set(false);
            metrics.recordAuditLatency(latencySample);
        }
    }

    public void shutdown() {
        auditExecutor.shutdown();
    }
}
