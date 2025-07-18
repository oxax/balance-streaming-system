package com.jpc.exercise.audit.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.audit.domain.service.BatchingAlgorithm;
import com.jpc.exercise.audit.infrastructure.persistence.AuditBatchPersistence;
import com.jpc.exercise.shared.audit.AuditNotifier;

public class AuditProcessingService {

    private static final int SUBMISSION_LIMIT = 1000;

    private final LinkedTransferQueue<Transaction> transactionQueue;
    private final BatchingAlgorithm batchingAlgorithm;
    private final AuditNotifier notifier;
    private final AuditBatchPersistence auditBatchPersistence;
    private final AtomicBoolean submitting = new AtomicBoolean(false);

    public AuditProcessingService(
            LinkedTransferQueue<Transaction> transactionQueue,
            BatchingAlgorithm batchingAlgorithm,
            AuditNotifier notifier,
            AuditBatchPersistence auditBatchPersistence) {
        this.transactionQueue = transactionQueue;
        this.batchingAlgorithm = batchingAlgorithm;
        this.notifier = notifier;
        this.auditBatchPersistence = auditBatchPersistence;
    }

    public void trySubmitIfThresholdMet() {
        if (transactionQueue.size() >= SUBMISSION_LIMIT && submitting.compareAndSet(false, true)) {
            new Thread(this::runAuditCycle).start();
        }
    }

    public void runAuditCycle() {
        try {
            List<Transaction> drained = new ArrayList<>();
            transactionQueue.drainTo(drained, SUBMISSION_LIMIT);

            if (drained.isEmpty()) return;

            String batchId = UUID.randomUUID().toString();
            auditBatchPersistence.save(batchId, drained);

            List<AuditBatch> batches = batchingAlgorithm.groupIntoBatches(drained);
            notifier.submit(batches);

            auditBatchPersistence.markSubmitted(batchId);
        } catch (Exception e) {
            System.err.println("⚠️ Audit cycle failed: " + e.getMessage());
        } finally {
            submitting.set(false);
        }
    }
}