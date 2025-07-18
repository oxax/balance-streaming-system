package com.jpc.exercise.producer;

import java.util.List;

import com.jpc.exercise.account.domain.model.Transaction;


@FunctionalInterface
public interface TransactionProducer {
    Transaction produce();
}
