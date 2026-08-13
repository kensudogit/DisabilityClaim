package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "unit_price_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitPriceMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "region_category_code", nullable = false, length = 20)
    private String regionCategoryCode;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "conversion_factor")
    private BigDecimal conversionFactor;

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
