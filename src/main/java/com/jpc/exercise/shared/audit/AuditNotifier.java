package com.jpc.exercise.shared.audit;

import java.util.List;

import com.jpc.exercise.audit.domain.model.AuditBatch;

public interface AuditNotifier {
    void submit(List<AuditBatch> batches);
}
