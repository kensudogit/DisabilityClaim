package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private FeeRuleSet ruleSet;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    private String description;

    private Integer units;

    private BigDecimal amount;

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
