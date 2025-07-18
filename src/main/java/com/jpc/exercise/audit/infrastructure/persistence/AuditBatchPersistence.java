package com.jpc.exercise.audit.infrastructure.persistence;

import com.jpc.exercise.account.domain.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface AuditBatchPersistence {

    void save(String batchId, List<Transaction> transactions);

    Optional<List<Transaction>> load(String batchId);

    void markSubmitted(String batchId);

    List<String> findPendingBatchIds();
}