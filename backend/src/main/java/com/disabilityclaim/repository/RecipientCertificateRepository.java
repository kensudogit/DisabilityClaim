package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.RecipientCertificate;
import com.disabilityclaim.domain.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecipientCertificateRepository extends JpaRepository<RecipientCertificate, UUID> {
    List<RecipientCertificate> findByBeneficiaryId(UUID beneficiaryId);

    List<RecipientCertificate> findByBeneficiaryIdAndServiceCategory(UUID beneficiaryId, ServiceCategory serviceCategory);

    @Query("""
            SELECT c FROM RecipientCertificate c
            WHERE c.beneficiary.id = :beneficiaryId
              AND c.serviceCategory = :serviceCategory
              AND c.validFrom <= :validTo
              AND c.validTo >= :validFrom
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    List<RecipientCertificate> findOverlapping(
            @Param("beneficiaryId") UUID beneficiaryId,
            @Param("serviceCategory") ServiceCategory serviceCategory,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("excludeId") UUID excludeId);

    @Query("""
            SELECT c FROM RecipientCertificate c
            WHERE c.validFrom <= :monthEnd
              AND c.validTo >= :monthStart
            """)
    List<RecipientCertificate> findValidInPeriod(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
