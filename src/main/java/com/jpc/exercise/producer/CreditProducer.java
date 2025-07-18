package com.jpc.exercise.producer;

import java.util.random.RandomGenerator;

import com.jpc.exercise.account.domain.model.Transaction;

public class CreditProducer implements TransactionProducer {

    private static final double MIN_AMOUNT = 200.0;
    private static final double MAX_AMOUNT = 500_000.0;
    private final RandomGenerator generator = RandomGenerator.getDefault();

    @Override
    public Transaction produce(){
        double amount = MIN_AMOUNT + (generator.nextDouble() * (MAX_AMOUNT - MIN_AMOUNT));
        return new Transaction(generator, Math.abs(amount));
    }
}