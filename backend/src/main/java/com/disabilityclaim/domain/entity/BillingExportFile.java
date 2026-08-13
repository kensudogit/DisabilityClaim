package com.disabilityclaim.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_export_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingExportFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "export_id", nullable = false)
    private BillingExport export;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "byte_size")
    private Long byteSize;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
