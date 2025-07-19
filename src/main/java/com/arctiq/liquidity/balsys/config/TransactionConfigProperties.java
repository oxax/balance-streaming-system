package com.arctiq.liquidity.balsys.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "transaction")
public class TransactionConfigProperties {

    private double minAmount;
    private double maxAmount;
    private double defaultBalance;
    private double maxBatchValue;

    public double getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(double minAmount) {
        this.minAmount = minAmount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getDefaultBalance() {
        return defaultBalance;
    }

    public void setDefaultBalance(double defaultBalance) {
        this.defaultBalance = defaultBalance;
    }

    public double getMaxBatchValue() {
        return maxBatchValue;
    }

    public void setMaxBatchValue(double maxBatchValue) {
        this.maxBatchValue = maxBatchValue;
    }
}
