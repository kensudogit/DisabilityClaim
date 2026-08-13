package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.ImportStagingRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImportStagingRowRepository extends JpaRepository<ImportStagingRow, UUID> {
    List<ImportStagingRow> findByImportJobId(UUID importJobId);

    List<ImportStagingRow> findByImportJobIdAndValidTrue(UUID importJobId);
}
