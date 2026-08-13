package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_calculation_traces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCalculationTrace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_case_id", nullable = false)
    private BillingCase billingCase;

    @Column(name = "rule_set_code", length = 100)
    private String ruleSetCode;

    @Column(name = "rule_version", length = 100)
    private String ruleVersion;

    @Column(name = "source_document", length = 500)
    private String sourceDocument;

    @Column(name = "inputs_json", nullable = false, columnDefinition = "TEXT")
    private String inputsJson;

    @Column(name = "steps_json", nullable = false, columnDefinition = "TEXT")
    private String stepsJson;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
