package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.UnitPriceMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface UnitPriceMasterRepository extends JpaRepository<UnitPriceMaster, UUID> {
    @Query("""
            SELECT u FROM UnitPriceMaster u
            WHERE u.regionCategoryCode = :region
              AND u.effectiveFrom <= :onDate
              AND (u.effectiveTo IS NULL OR u.effectiveTo >= :onDate)
            """)
    List<UnitPriceMaster> findEffectiveOn(
            @Param("region") String regionCategoryCode,
            @Param("onDate") LocalDate onDate);
}
