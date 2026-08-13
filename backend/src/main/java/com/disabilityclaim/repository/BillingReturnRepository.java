package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillingReturnRepository extends JpaRepository<BillingReturn, UUID> {
}
