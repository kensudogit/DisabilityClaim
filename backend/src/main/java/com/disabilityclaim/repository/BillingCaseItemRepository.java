package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingCaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingCaseItemRepository extends JpaRepository<BillingCaseItem, UUID> {
    List<BillingCaseItem> findByBillingCaseId(UUID billingCaseId);

    void deleteByBillingCaseId(UUID billingCaseId);
}
