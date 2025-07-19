package com.arctiq.liquidity.balsys.audit.dispatch;

import java.util.List;

import com.arctiq.liquidity.balsys.audit.domain.AuditBatch;
import com.arctiq.liquidity.balsys.shared.audit.AuditNotifier;

public class LocalAuditNotifier implements AuditNotifier {

    @Override
    public void submit(List<AuditBatch> batches) {
        for (AuditBatch batch : batches) {
            System.out.println("Processing batch ID: " + batch.getBatchId() + " with " + batch.getTransactionCount()
                    + " transactions.");
        }
    }
}
