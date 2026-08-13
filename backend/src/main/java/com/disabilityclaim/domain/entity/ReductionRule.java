package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reduction_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReductionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reduction_code", nullable = false, length = 50)
    private String reductionCode;

    @Column(name = "reduction_name", nullable = false, length = 200)
    private String reductionName;

    @Column(name = "threshold_value")
    private Integer thresholdValue;

    @Column(name = "reduction_units")
    private Integer reductionUnits;

    @Column(name = "reduction_rate")
    private BigDecimal reductionRate;

    @Column(name = "condition_json", columnDefinition = "TEXT")
    private String conditionJson;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "source_document", nullable = false, length = 500)
    private String sourceDocument;

    @Column(name = "source_version", nullable = false, length = 100)
    private String sourceVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
