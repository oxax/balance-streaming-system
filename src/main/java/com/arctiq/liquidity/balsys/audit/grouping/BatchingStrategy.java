package com.arctiq.liquidity.balsys.audit.grouping;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;

import java.util.List;

public interface BatchingStrategy {
    List<AuditBatch> groupIntoBatches(List<Transaction> transactions);
}