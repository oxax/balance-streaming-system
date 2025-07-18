package com.jpc.exercise.shared.observability;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jpc.exercise.account.domain.model.Transaction;
import com.jpc.exercise.audit.domain.model.AuditBatch;

public class MetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);

    public void recordTransaction(Transaction tx) {
        logger.info("Transaction recorded at {} | Type: {} | Amount: {}",
            Instant.now(),
            tx.getAmount() > 0 ? "Credit" : "Debit",
            String.format("%.2f", tx.getAmount())
        );
    }

    public void recordBalance(double balance) {
        logger.info("Balance updated at {} | New Balance: {}",
            Instant.now(),
            String.format("%.2f", balance)
        );
    }

    public void recordAuditSubmission(List<AuditBatch> batches) {
        logger.info("Audit submission at {} | Submitted {} batches",
            Instant.now(),
            batches.size()
        );
    }
}
