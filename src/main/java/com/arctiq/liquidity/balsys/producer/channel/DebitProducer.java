package com.arctiq.liquidity.balsys.producer.channel;

import java.util.random.RandomGenerator;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.config.TransactionConfigProperties;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public class DebitProducer implements TransactionProducer {

    private final RandomGenerator generator = RandomGenerator.getDefault();
    private final TransactionConfigProperties config;

    public DebitProducer(TransactionConfigProperties config) {
        this.config = config;
    }

    @Override
    public Transaction produce() {
        double amount = config.getMinAmount()
                + generator.nextDouble() * (config.getMaxAmount() - config.getMinAmount());
        return new Transaction(TransactionId.generate(), Money.of(-Math.abs(amount)));
    }
}
