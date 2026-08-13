package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingCalculationTrace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingCalculationTraceRepository extends JpaRepository<BillingCalculationTrace, UUID> {
    List<BillingCalculationTrace> findByBillingCaseId(UUID billingCaseId);
}
