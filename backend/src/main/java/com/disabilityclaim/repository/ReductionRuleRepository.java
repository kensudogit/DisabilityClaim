package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.ReductionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReductionRuleRepository extends JpaRepository<ReductionRule, UUID> {
    @Query("""
            SELECT r FROM ReductionRule r
            WHERE r.effectiveFrom <= :onDate
              AND (r.effectiveTo IS NULL OR r.effectiveTo >= :onDate)
            """)
    List<ReductionRule> findEffectiveOn(@Param("onDate") LocalDate onDate);
}
