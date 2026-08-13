package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_case_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_case_id", nullable = false)
    private BillingCase billingCase;

    @Column(name = "item_type", nullable = false, length = 40)
    private String itemType;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @Column(name = "item_name", length = 200)
    private String itemName;

    private Integer units;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    private BigDecimal amount;

    @Column(name = "rule_code", length = 100)
    private String ruleCode;

    @Column(name = "rule_version", length = 100)
    private String ruleVersion;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
