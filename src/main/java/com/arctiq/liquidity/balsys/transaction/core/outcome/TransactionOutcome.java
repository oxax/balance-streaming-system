package com.arctiq.liquidity.balsys.transaction.core.outcome;

import com.arctiq.liquidity.balsys.transaction.core.Transaction;

public sealed interface TransactionOutcome
        permits TransactionAccepted, TransactionInvalid {

    Transaction transaction();

    String reason();
}