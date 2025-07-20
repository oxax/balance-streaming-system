package com.arctiq.liquidity.balsys.audit.ingestion;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.audit.grouping.BatchingStrategy;
import com.arctiq.liquidity.balsys.audit.persistence.AuditBatchPersistence;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;
import com.arctiq.liquidity.balsys.telemetry.metrics.MetricsCollector;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AuditProcessingService.class);

    private final BlockingQueue<Transaction> transactionQueue;
    private final BatchingStrategy batchingStrategy;
    private final AuditNotifier notifier;
    private final AuditBatchPersistence auditBatchPersistence;
    private final MetricsCollector metrics;

    private final ExecutorService auditExecutor;
    private final ScheduledExecutorService scheduler;

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
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(this::flushDueToTimeout,
                config.getFlushIntervalMillis(),
                config.getFlushIntervalMillis(),
                TimeUnit.MILLISECONDS);

        logger.info("AuditProcessingService initialized:");
        logger.info("  - Queue capacity: {}", config.getQueueCapacity());
        logger.info("  - Submission limit: {}", config.getSubmissionLimit());
        logger.info("  - Flush interval (ms): {}", config.getFlushIntervalMillis());
        logger.info("  - Audit threads: {}", config.getAuditThreads());
    }

    public boolean ingest(Transaction tx) {
        logger.debug("Ingesting transaction: {}", tx);
        boolean accepted = transactionQueue.offer(tx);
        metrics.updateQueueSize(transactionQueue.size());

        if (!accepted) {
            logger.warn("Queue saturated. Dropping transaction: {}", tx);
            persistDropped(tx);
            metrics.incrementDropped(tx);
        }
        return accepted;
    }

    public void flushIfThresholdMet() {
        logger.info("Checking submission threshold: queueSize={}, submissionLimit={}", transactionQueue.size(),
                submissionLimit);
        if (transactionQueue.size() >= submissionLimit && submitting.compareAndSet(false, true)) {
            logger.info("Submission threshold met. Triggering audit flush.");
            auditExecutor.submit(this::runAuditCycle);
        }
    }

    private void flushDueToTimeout() {
        logger.info("Scheduled flush check: queueSize={}", transactionQueue.size());
        if (!transactionQueue.isEmpty() && submitting.compareAndSet(false, true)) {
            logger.info("Scheduled flush triggered. Submitting audit cycle.");
            auditExecutor.submit(this::runAuditCycle);
        }
    }

    private void runAuditCycle() {
        logger.info("Starting audit cycle...");
        Timer.Sample latencySample = metrics.startAuditLatencySample();

        try {
            List<Transaction> drained = new ArrayList<>();
            transactionQueue.drainTo(drained, submissionLimit);
            metrics.updateQueueSize(transactionQueue.size());

            if (drained.isEmpty()) {
                logger.info("Audit cycle skipped. No transactions drained.");
                return;
            }

            String batchId = UUID.randomUUID().toString();
            logger.info("Persisting audit batch [{}] with {} transactions", batchId, drained.size());
            auditBatchPersistence.save(batchId, drained);

            List<AuditBatch> grouped = batchingStrategy.groupIntoBatches(drained);
            logger.info("Grouped into {} sub-batches for submission", grouped.size());
            notifier.submit(grouped);

            auditBatchPersistence.markSubmitted(batchId);
            logger.info("Audit batch [{}] submitted. Transaction count: {}, sub-batches: {}", batchId, drained.size(),
                    grouped.size());

        } catch (Exception e) {
            logger.error("Audit cycle failure: {}", e.getMessage(), e);
        } finally {
            submitting.set(false);
            metrics.recordAuditLatency(latencySample);
            logger.info("Audit cycle complete. Submission flag cleared.");
        }
    }

    private void persistDropped(Transaction tx) {
        String dropId = "dropped-" + UUID.randomUUID();
        logger.info("Persisting dropped transaction [{}]", dropId);
        try {
            auditBatchPersistence.save(dropId, Collections.singletonList(tx));
        } catch (Exception e) {
            logger.error("Failed to persist dropped transaction [{}]: {}", dropId, e.getMessage(), e);
        }
    }

    public void shutdown() {
        logger.info("Initiating shutdown of AuditProcessingService...");
        scheduler.shutdown();
        auditExecutor.shutdown();
        logger.info("AuditProcessingService shut down completed.");
    }
}