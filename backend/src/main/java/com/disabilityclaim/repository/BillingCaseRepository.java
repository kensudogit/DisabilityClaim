package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingCaseRepository extends JpaRepository<BillingCase, UUID> {
    List<BillingCase> findByBatchId(UUID batchId);

    void deleteByBatchId(UUID batchId);
}
