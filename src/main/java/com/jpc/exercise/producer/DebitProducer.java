package com.jpc.exercise.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import com.jpc.exercise.account.domain.model.Transaction;

public class DebitProducer implements TransactionProducer {

    private static final long MIN_AMOUNT = 200L;
    private static final long MAX_AMOUNT = 500_000L;
    private final RandomGenerator generator = RandomGenerator.getDefault();

    @Override
    public Transaction produce() {
        long amount = MIN_AMOUNT + (long) (generator.nextDouble() * (MAX_AMOUNT - MIN_AMOUNT));
        return new Transaction(generator, -Math.abs(amount));
    }


}