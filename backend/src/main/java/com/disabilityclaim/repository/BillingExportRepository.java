package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingExport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingExportRepository extends JpaRepository<BillingExport, UUID> {
    List<BillingExport> findByBatchId(UUID batchId);
}
