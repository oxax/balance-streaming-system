package com.arctiq.liquidity.balsys.producer.channel;

import java.util.random.RandomGenerator;

import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;
import com.arctiq.liquidity.balsys.transaction.core.Transaction;
import com.arctiq.liquidity.balsys.transaction.core.TransactionId;

public class CreditProducer implements TransactionProducer {

    private final RandomGenerator generator = RandomGenerator.getDefault();
    private final TransactionConfigProperties config;

    public CreditProducer(TransactionConfigProperties config) {
        this.config = config;
    }

    @Override
    public Transaction produce() {
        double amount = config.getMinAmount()
                + generator.nextDouble() * (config.getMaxAmount() - config.getMinAmount());
        return new Transaction(TransactionId.generate(), Money.of(Math.abs(amount)));
    }
}
