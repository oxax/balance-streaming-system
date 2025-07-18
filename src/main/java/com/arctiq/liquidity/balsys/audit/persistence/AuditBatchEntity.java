package com.arctiq.liquidity.balsys.audit.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_batches")
public class AuditBatchEntity {

    @Id
    private String id;

    @ElementCollection
    @CollectionTable(name = "audit_batch_transactions", joinColumns = @JoinColumn(name = "batch_id"))
    @Column(name = "amount")
    private List<Long> transactionAmounts = new ArrayList<>();

    private long totalValue;

    private boolean submitted;

    private LocalDateTime createdAt;

    protected AuditBatchEntity() {}

    public AuditBatchEntity(String id, List<Long> amounts, long totalValue, boolean submitted) {
        this.id = id;
        this.transactionAmounts = amounts;
        this.totalValue = totalValue;
        this.submitted = submitted;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public List<Long> getTransactionAmounts() {
        return transactionAmounts;
    }

    public long getTotalValue() {
        return totalValue;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}