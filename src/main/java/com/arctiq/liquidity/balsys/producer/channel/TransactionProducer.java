package com.arctiq.liquidity.balsys.producer.channel;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;

@FunctionalInterface
public interface TransactionProducer {
    Transaction produce();
}
