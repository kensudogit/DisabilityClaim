package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.SupportActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportActivityRepository extends JpaRepository<SupportActivity, UUID> {
    List<SupportActivity> findByBillingMonth(String billingMonth);

    List<SupportActivity> findByBeneficiaryIdAndBillingMonth(UUID beneficiaryId, String billingMonth);

    boolean existsByBeneficiaryIdAndBillingMonth(UUID beneficiaryId, String billingMonth);
}
