package com.arctiq.liquidity.balsys.transaction.core.outcome;

import com.arctiq.liquidity.balsys.transaction.core.Transaction;

public final record TransactionAccepted(Transaction transaction) implements TransactionOutcome {
    @Override
    public String reason() {
        return "Accepted";
    }
}