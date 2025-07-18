package com.jpc.exercise.audit.infrastructure.notifier;

import java.util.List;

import com.jpc.exercise.audit.domain.model.AuditBatch;
import com.jpc.exercise.shared.audit.AuditNotifier;

public class LocalAuditNotifier implements AuditNotifier {

    @Override
    public void submit(List<AuditBatch> batches) {
        for (AuditBatch batch : batches) {
            System.out.println("Processing batch ID: " + batch.getBatchId() + " with " + batch.getTransactionCount() + " transactions.");
        }
    }
    
}
