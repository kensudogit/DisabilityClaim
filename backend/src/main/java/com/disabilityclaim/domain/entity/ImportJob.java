package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "valid_rows", nullable = false)
    private int validRows;

    @Column(name = "error_rows", nullable = false)
    private int errorRows;

    @Column(name = "allow_partial", nullable = false)
    private boolean allowPartial;

    @Column(nullable = false)
    private boolean committed;

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
        if (jobType == null) {
            jobType = "EXCEL_BENEFICIARY";
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
