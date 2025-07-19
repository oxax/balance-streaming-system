package com.arctiq.liquidity.balsys.transaction.core.outcome;

import com.arctiq.liquidity.balsys.transaction.core.Transaction;

public final record TransactionInvalid(Transaction transaction, String reason) implements TransactionOutcome {
}