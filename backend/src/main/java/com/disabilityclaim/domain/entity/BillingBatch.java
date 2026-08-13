package com.disabilityclaim.domain.entity;

import com.disabilityclaim.domain.enums.BillingBatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "office_id", nullable = false)
    private OfficeProfile office;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillingBatchStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_rule_set_id")
    private FeeRuleSet feeRuleSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private UserAccount confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    private String notes;

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
            status = BillingBatchStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
