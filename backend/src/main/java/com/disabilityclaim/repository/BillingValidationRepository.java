package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingValidation;
import com.disabilityclaim.domain.enums.ValidationSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingValidationRepository extends JpaRepository<BillingValidation, UUID> {
    List<BillingValidation> findByBatchId(UUID batchId);

    boolean existsByBatchIdAndSeverity(UUID batchId, ValidationSeverity severity);

    void deleteByBatchId(UUID batchId);
}
