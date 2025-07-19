package com.arctiq.liquidity.balsys.producer.channel;

import java.util.random.RandomGenerator;

import com.arctiq.liquidity.balsys.account.domain.model.Transaction;
import com.arctiq.liquidity.balsys.account.domain.model.TransactionId;
import com.arctiq.liquidity.balsys.shared.domain.model.Money;

public class DebitProducer implements TransactionProducer {

    private static final double MIN_AMOUNT = 200.0;
    private static final double MAX_AMOUNT = 500_000.0;
    private final RandomGenerator generator = RandomGenerator.getDefault();

    @Override
    public Transaction produce() {
        double amount = MIN_AMOUNT + generator.nextDouble() * (MAX_AMOUNT - MIN_AMOUNT);
        return new Transaction(TransactionId.generate(), Money.of(-Math.abs(amount)));
    }
}
