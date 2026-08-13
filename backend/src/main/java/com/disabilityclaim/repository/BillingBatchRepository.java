package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingBatchRepository extends JpaRepository<BillingBatch, UUID> {
    Optional<BillingBatch> findByOfficeIdAndBillingMonth(UUID officeId, String billingMonth);
}
