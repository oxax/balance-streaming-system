package com.arctiq.liquidity.balsys.shared.audit;

import java.util.List;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;

public interface AuditNotifier {
    void submit(List<AuditBatch> batches);
}
