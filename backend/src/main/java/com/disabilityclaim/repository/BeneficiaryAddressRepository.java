package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BeneficiaryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeneficiaryAddressRepository extends JpaRepository<BeneficiaryAddress, UUID> {
    List<BeneficiaryAddress> findByBeneficiaryId(UUID beneficiaryId);
}
