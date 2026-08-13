package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.ServiceCodeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ServiceCodeMasterRepository extends JpaRepository<ServiceCodeMaster, UUID> {
    @Query("""
            SELECT s FROM ServiceCodeMaster s
            WHERE s.effectiveFrom <= :onDate
              AND (s.effectiveTo IS NULL OR s.effectiveTo >= :onDate)
            """)
    List<ServiceCodeMaster> findEffectiveOn(@Param("onDate") LocalDate onDate);
}
