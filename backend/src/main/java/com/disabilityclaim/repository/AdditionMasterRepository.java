package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.AdditionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AdditionMasterRepository extends JpaRepository<AdditionMaster, UUID> {
    @Query("""
            SELECT a FROM AdditionMaster a
            WHERE a.effectiveFrom <= :onDate
              AND (a.effectiveTo IS NULL OR a.effectiveTo >= :onDate)
            """)
    List<AdditionMaster> findEffectiveOn(@Param("onDate") LocalDate onDate);
}
