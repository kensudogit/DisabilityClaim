package com.disabilityclaim.domain.entity;

import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BillingCaseStatus;
import com.disabilityclaim.domain.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private BillingBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private Beneficiary beneficiary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id")
    private RecipientCertificate certificate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id")
    private Municipality municipality;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", length = 40)
    private ServiceCategory serviceCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BeneficiaryCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BillingCaseStatus status;

    @Column(name = "total_units")
    private Integer totalUnits;

    @Column(name = "billed_amount")
    private BigDecimal billedAmount;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = BillingCaseStatus.CANDIDATE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
