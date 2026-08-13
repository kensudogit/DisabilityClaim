package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.FeeRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FeeRuleSetRepository extends JpaRepository<FeeRuleSet, UUID> {
    @Query("""
            SELECT r FROM FeeRuleSet r
            WHERE r.effectiveFrom <= :onDate
              AND (r.effectiveTo IS NULL OR r.effectiveTo >= :onDate)
            ORDER BY r.effectiveFrom DESC
            """)
    List<FeeRuleSet> findEffectiveOn(@Param("onDate") LocalDate onDate);
}
