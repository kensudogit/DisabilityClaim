package com.disabilityclaim.domain.entity;

import com.disabilityclaim.domain.enums.ValidationSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingValidation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private BillingBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_case_id")
    private BillingCase billingCase;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
