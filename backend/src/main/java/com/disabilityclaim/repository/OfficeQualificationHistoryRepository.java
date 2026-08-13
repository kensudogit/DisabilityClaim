package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.OfficeQualificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfficeQualificationHistoryRepository extends JpaRepository<OfficeQualificationHistory, UUID> {
    List<OfficeQualificationHistory> findByOfficeId(UUID officeId);
}
