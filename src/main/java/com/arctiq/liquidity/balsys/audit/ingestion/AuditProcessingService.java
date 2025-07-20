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

        logger.info("AuditProcessingService initialized:");
        logger.info("  - Queue capacity: {}", config.getQueueCapacity());
        logger.info("  - Submission limit: {}", config.getSubmissionLimit());
        logger.info("  - Audit threads: {}", config.getAuditThreads());
        logger.info("  ❎ No scheduler is configured — service is passive until triggered.");
    }

    /** Accept transaction and conditionally trigger processing **/
    public boolean ingest(Transaction tx) {
        logger.debug("Ingesting transaction: {}", tx);
        boolean accepted = transactionQueue.offer(tx);
        metrics.updateQueueSize(transactionQueue.size());

        if (!accepted) {
            logger.warn("Queue saturated. Dropping transaction: {}", tx);
            persistDropped(tx);
            metrics.incrementDropped(tx);
            return false;
        }

        flushIfThresholdMet();
        return true;
    }

    /** Trigger processing if queue exceeds threshold **/
    public void flushIfThresholdMet() {
        logger.info("Threshold check: queueSize={}, submissionLimit={}", transactionQueue.size(), submissionLimit);
        if (transactionQueue.size() >= submissionLimit && submitting.compareAndSet(false, true)) {
            logger.debug("Submission threshold met. Running audit cycle.");
            auditExecutor.submit(this::runAuditCycle);
        }
    }

    /** Audit batching and persistence cycle **/
    private void runAuditCycle() {
        logger.info("Starting audit cycle...");
        Timer.Sample latencySample = metrics.startAuditLatencySample();

        try {
            List<Transaction> drained = new ArrayList<>();
            transactionQueue.drainTo(drained, submissionLimit);
            metrics.updateQueueSize(transactionQueue.size());

            if (drained.isEmpty()) {
                logger.info("No transactions to process. Skipping batch.");
                return;
            }

            String batchId = UUID.randomUUID().toString();
            logger.info("Persisting audit batch [{}] with {} transactions", batchId, drained.size());
            auditBatchPersistence.save(batchId, drained);

            List<AuditBatch> grouped = batchingStrategy.groupIntoBatches(drained);
            logger.info("Grouped into {} sub-batches for submission", grouped.size());
            notifier.submit(grouped);

            auditBatchPersistence.markSubmitted(batchId);
            logger.info("Audit batch [{}] submitted. Transaction count: {}, sub-batches: {}",
                    batchId, drained.size(), grouped.size());

        } catch (Exception e) {
            logger.error("Audit cycle failure: {}", e.getMessage(), e);
        } finally {
            submitting.set(false);
            metrics.recordAuditLatency(latencySample);
            logger.info("Audit cycle completed. Submission lock released.");
        }
    }

    /** Persist dropped transactions when queue is full **/
    private void persistDropped(Transaction tx) {
        String dropId = "dropped-" + UUID.randomUUID();
        logger.info("Persisting dropped transaction [{}]", dropId);
        try {
            auditBatchPersistence.save(dropId, Collections.singletonList(tx));
        } catch (Exception e) {
            logger.error("Failed to persist dropped transaction [{}]: {}", dropId, e.getMessage(), e);
        }
    }

    /** Graceful shutdown method (e.g. called on lifecycle hook) **/
    public void shutdown() {
        logger.info("Initiating shutdown of AuditProcessingService...");
        auditExecutor.shutdown();
        logger.info("AuditProcessingService shutdown complete.");
    }
}
