package com.arctiq.liquidity.balsys.audit.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditBatchRepository extends JpaRepository<AuditBatchEntity, String> {
    List<AuditBatchEntity> findBySubmittedFalse();
}