package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "addition_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "addition_code", nullable = false, length = 50)
    private String additionCode;

    @Column(name = "addition_name", nullable = false, length = 200)
    private String additionName;

    private Integer units;

    private BigDecimal amount;

    @Column(name = "auto_applicable", nullable = false)
    private boolean autoApplicable;

    @Column(name = "requires_manual_confirm", nullable = false)
    @Builder.Default
    private boolean requiresManualConfirm = true;

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
