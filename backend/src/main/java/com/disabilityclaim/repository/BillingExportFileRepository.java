package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.BillingExportFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BillingExportFileRepository extends JpaRepository<BillingExportFile, UUID> {
}
