package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_exports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingExport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private BillingBatch batch;

    @Column(name = "export_type", nullable = false, length = 50)
    private String exportType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "adapter_name", length = 100)
    private String adapterName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (exportType == null) {
            exportType = "KOKUHO";
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
