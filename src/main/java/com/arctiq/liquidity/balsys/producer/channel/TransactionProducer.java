package com.arctiq.liquidity.balsys.producer.channel;

import com.arctiq.liquidity.balsys.transaction.core.Transaction;

@FunctionalInterface
public interface TransactionProducer {
    Transaction produce();
}
