package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_staging_rows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportStagingRow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "raw_json", nullable = false, columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "mapped_json", columnDefinition = "TEXT")
    private String mappedJson;

    @Column(nullable = false)
    private boolean valid;

    @Column(name = "error_column", length = 100)
    private String errorColumn;

    @Column(name = "error_reason", columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
