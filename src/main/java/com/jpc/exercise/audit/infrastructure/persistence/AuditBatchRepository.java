package com.jpc.exercise.audit.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditBatchRepository extends JpaRepository<AuditBatchEntity, String> {
    List<AuditBatchEntity> findBySubmittedFalse();
}