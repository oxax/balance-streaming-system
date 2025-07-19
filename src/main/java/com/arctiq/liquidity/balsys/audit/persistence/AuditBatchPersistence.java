package com.arctiq.liquidity.balsys.audit.persistence;

import java.util.List;
import java.util.Optional;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;

public interface AuditBatchPersistence {

    void save(String batchId, List<Transaction> transactions);

    Optional<List<Transaction>> load(String batchId);

    void markSubmitted(String batchId);

    List<String> findPendingBatchIds();
}