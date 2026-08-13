package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "billing_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private BillingBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_case_id")
    private BillingCase billingCase;

    @Column(name = "return_code", length = 50)
    private String returnCode;

    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;

    @Column(name = "returned_at")
    private LocalDate returnedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
